# JettoCode 启动指南

## 环境变量配置

### JETTO_CODE_WORKSPACE 环境变量

JettoCode 使用 `JETTO_CODE_WORKSPACE` 环境变量来指定所有数据存储的位置。

**默认值**：`/opt/Projects/JettoCode/workspace`

**目录结构**：
```
$JETTO_CODE_WORKSPACE/
├── repos/                    # Git 仓库克隆目录
│   ├── services/             # 服务代码仓库
│   │   ├── user-service/
│   │   ├── order-service/
│   │   └── payment-service/
│   └── projects/             # 项目代码仓库
│       ├── frontend/
│       └── code/
└── database/                 # 数据库文件（如使用H2）
```

## 配置方式

### 方式一：使用默认WORKSPACE

```bash
# 使用默认的 workspace 目录
./startup.sh
```

### 方式二：自定义WORKSPACE

```bash
# 方式1：临时设置
JETTO_CODE_WORKSPACE=/path/to/your/workspace ./startup.sh

# 方式2：环境变量文件
export JETTO_CODE_WORKSPACE=/path/to/your/workspace
./startup.sh

# 方式3：永久设置（添加到 ~/.bashrc 或 ~/.zshrc）
echo 'export JETTO_CODE_WORKSPACE=/path/to/your/workspace' >> ~/.bashrc
source ~/.bashrc
./startup.sh
```

### 方式三：使用.env文件

创建 `.env` 文件：
```bash
# 在 JettoCode 根目录创建 .env 文件
JETTO_CODE_WORKSPACE=/path/to/your/workspace
```

## 后端配置

后端通过以下优先级确定WORKSPACE路径：

1. **配置文件** (`application.yml`):
   ```yaml
   jettech:
     workspace: /custom/workspace/path
   ```

2. **环境变量**:
   ```bash
   export JETTO_CODE_WORKSPACE=/custom/workspace/path
   ```

3. **默认路径**: `/opt/Projects/JettoCode/workspace`

## Git操作配置

在 `application.yml` 中配置Git操作参数：

```yaml
jettech:
  git:
    timeout: 300000          # Git操作超时时间（毫秒），默认5分钟
    retry-times: 3           # 失败重试次数
    retry-interval: 5000     # 重试间隔（毫秒）
    shallow-depth: 1         # 浅克隆深度（仅克隆最新提交）
```

## 数据持久化

所有数据都存储在 `$JETTO_CODE_WORKSPACE` 目录中：

- **代码仓库**: `$JETTO_CODE_WORKSPACE/repos/`
- **数据库**: `$JETTO_CODE_WORKSPACE/database/` (如使用H2)
- **日志文件**: `$JETTO_CODE_WORKSPACE/logs/`
- **临时文件**: `$JETTO_CODE_WORKSPACE/tmp/`

## 备份和迁移

### 备份

```bash
# 备份整个workspace
tar -czf jettocode-backup-$(date +%Y%m%d).tar.gz -C $(dirname $JETTO_CODE_WORKSPACE) $(basename $JETTO_CODE_WORKSPACE)
```

### 迁移

```bash
# 1. 停止服务
./shutdown.sh

# 2. 复制workspace到新位置
cp -r $JETTO_CODE_WORKSPACE /new/location/workspace

# 3. 更新环境变量
export JETTO_CODE_WORKSPACE=/new/location/workspace

# 4. 启动服务
./startup.sh
```

## Docker部署

使用Docker时，通过挂载卷来持久化数据：

```bash
docker run -d \
  -e JETTO_CODE_WORKSPACE=/data \
  -v /host/path/workspace:/data \
  jettocode/code:latest
```

## 常见问题

### Q: WORKSPACE目录权限问题

```bash
# 确保当前用户对workspace目录有读写权限
sudo chown -R $USER:$USER $JETTO_CODE_WORKSPACE
chmod -R 755 $JETTO_CODE_WORKSPACE
```

### Q: Git克隆失败

1. 检查网络连接
2. 增加超时时间 (`jettech.git.timeout`)
3. 检查磁盘空间

### Q: 路径中包含空格

确保WORKSPACE路径不包含空格，或使用引号：

```bash
export JETTO_CODE_WORKSPACE="/path/with spaces/workspace"
```

## 开发模式

开发模式下，建议使用项目目录中的workspace：

```bash
# 使用项目内的workspace目录
export JETTO_CODE_WORKSPACE=/opt/Projects/JettoCode/workspace
./startup.sh
```

## 生产环境建议

1. **独立磁盘**: 将workspace放在独立磁盘分区
2. **定期备份**: 设置自动备份策略
3. **监控磁盘**: 监控workspace目录的磁盘使用情况
4. **权限管理**: 限制对workspace目录的访问权限
