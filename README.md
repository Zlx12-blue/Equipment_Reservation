# 实验室设备预约审批系统

基于 **Spring Boot 3 + Flowable 7** 工作流引擎的多端实验室设备预约审批系统。

## 系统架构

```
┌──────────────┐      HTTP/REST       ┌────────────────────────────────┐
│  学生端(Web) │ ───────────────────► │                                │
└──────────────┘                      │   Spring Boot 后端 (8080)       │
┌──────────────┐      HTTP/REST       │   ├── 认证 / 设备 / 预约 REST   │
│ 教师/管理端  │ ───────────────────► │   ├── Flowable 流程引擎(审批流)│
│   (Web)      │ ◄─────────────────── │   └── WebSocket 实时通知        │
└──────────────┘   WebSocket 推送     └──────────────┬─────────────────┘
                                                     │ JDBC
                                        ┌────────────▼─────────────────┐
                                        │   MySQL 8(业务表 + 流程表)   │
                                        └──────────────────────────────┘
```

## 多端互动流程(对应验收标准 2)

1. 学生端提交设备预约申请 → 后端启动 Flowable 流程实例,数据写入 MySQL
2. **教师/管理端通过 WebSocket 实时收到"新的待审批"消息**(买家下单 → 卖家收到订单)
3. 教师审批通过/驳回 → Flowable 流转 → **学生端实时收到审批结果通知**

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2、Flowable 7.0(BPMN 审批流)、Spring Data JPA、Spring Security + JWT、WebSocket |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios |
| 数据库 | MySQL 8 |
| 部署 | Docker + docker-compose,Nginx 托管前端并反向代理后端 |

## 快速开始(本地开发)

```bash
# 1. 启动 MySQL(或直接用 docker)
docker run -d --name lab-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=lab_reservation mysql:8

# 2. 启动后端(自动建表 + 自动部署 BPMN 流程 + 初始化测试账号)
cd backend && mvn spring-boot:run

# 3. 启动前端
cd frontend && npm install && npm run dev
```

测试账号(密码均为 `123456`):

| 账号 | 角色 |
|---|---|
| student1 | 学生(预约设备、查看进度) |
| teacher1 | 教师(审批预约) |
| admin | 管理员(设备管理 + 审批) |

## 部署到服务器(对应验收标准 4)

```bash
# 在云服务器上执行(需安装 docker 与 docker compose)
git clone <你的仓库地址> && cd lab-reservation-system
docker compose up -d --build
```

- 前端访问:`http://<服务器IP>/`
- 后端 API:`http://<服务器IP>/api/`
- 云服务器安全组需放行 80 端口,即可实现不同网络访问。

## 团队分工与 Git 提交建议(对应验收标准 3)

| 成员 | 负责模块 | 提交目录 |
|---|---|---|
| 成员 A | 后端:实体 / 仓库 / 设备与预约 REST | `backend/src/.../entity,repository,controller` |
| 成员 B | 后端:Flowable 流程定义与审批服务 / 通知 | `backend/.../service,resources/processes` |
| 成员 C | 前端:学生端页面 + API 封装 | `frontend/src/views/student,api` |
| 成员 D | 前端:管理端页面 + 部署(Docker/Nginx) | `frontend/src/views/admin,docker*` |

约定:每人基于 `main` 切功能分支(如 `feat/device-crud`),通过 Pull Request 合并,保证远程仓库中每个人都有独立提交记录。

## 验收标准对照

1. ✅ 前端增删改 → REST → 后端业务处理 → MySQL 落库(设备管理 / 预约全流程)
2. ✅ 多端互动:学生提交预约 → 教师端 WebSocket 实时收到待办;审批 → 学生端实时收到结果
3. ✅ 按上表分工提交,远程仓库可查证每人提交记录
4. ✅ docker-compose 一键部署至云服务器,公网可访问
