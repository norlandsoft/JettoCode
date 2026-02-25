# 代码分析平台

一个基于Vue3和Spring Boot的代码分析平台，支持从Git仓库克隆项目、分析项目架构、展示代码、绘制业务流程和调用链路、追踪代码片段调用、以及版本差异分析。

## 功能特性

- **Git项目管理**: 克隆、管理多个Git仓库
- **代码浏览**: 树形结构展示项目文件，代码高亮显示
- **项目架构分析**: 分析并可视化项目的包结构、类关系
- **业务流程图**: 自动生成代码对应的业务流程图
- **调用链路追踪**: 可视化方法调用关系
- **接口追踪**: 从代码片段反向查找调用接口
- **版本Diff分析**: 比较不同提交版本，识别受影响的接口

## 技术栈

### 前端
- Vue 3 + TypeScript
- Ant Design Vue (UI组件库)
- Monaco Editor (代码编辑器)
- ECharts (图表可视化)
- Vite (构建工具)
- Pinia (状态管理)
- Vue Router (路由)

### 后端
- Spring Boot 3.2
- JGit (Git操作)
- H2 Database (内存数据库)

## 项目结构

```
.
├── code/                       # 后端Spring Boot项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/jettech/code/
│   │   │   │   ├── controller/    # REST API控制器
│   │   │   │   ├── service/       # 业务逻辑服务
│   │   │   │   ├── repository/    # 数据访问层
│   │   │   │   ├── model/         # 实体类
│   │   │   │   ├── dto/           # 数据传输对象
│   │   │   │   ├── config/        # 配置类
│   │   │   │   └── exception/     # 异常处理
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   └── pom.xml
│
├── frontend/                   # 前端Vue3项目
│   ├── src/
│   │   ├── api/               # API请求
│   │   ├── components/        # 通用组件
│   │   ├── views/             # 页面组件
│   │   ├── router/            # 路由配置
│   │   ├── types/             # TypeScript类型定义
│   │   ├── utils/             # 工具函数
│   │   └── assets/            # 静态资源
│   ├── package.json
│   └── vite.config.ts
│
└── README.md
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- Maven 3.8+

### 启动后端

```bash
cd code
mvn clean install
java -jar target/code-analyzer-1.0.0-SNAPSHOT.jar
```

后端服务将在 `http://localhost:9990` 启动

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:9999` 启动

### 访问应用

打开浏览器访问 `http://localhost:9999`

## API接口

### 项目管理

- `POST /api/projects/clone` - 克隆Git仓库
- `GET /api/projects` - 获取所有项目
- `GET /api/projects/{id}` - 获取项目详情
- `GET /api/projects/{id}/branches` - 获取项目分支列表
- `POST /api/projects/{id}/checkout?branch={branch}` - 切换分支
- `POST /api/projects/{id}/pull` - 拉取最新代码
- `DELETE /api/projects/{id}` - 删除项目

## 开发计划

- [x] 项目框架搭建
- [x] Git仓库克隆与管理
- [ ] 代码浏览与展示
- [ ] 项目架构分析
- [ ] 业务流程图生成
- [ ] 调用链路可视化
- [ ] 接口追踪功能
- [ ] 版本Diff分析
- [ ] 代码静态分析

## 贡献指南

欢迎提交Issue和Pull Request

## 许可证

MIT License
