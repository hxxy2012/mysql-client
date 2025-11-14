# MySQL Client - JavaFX桌面应用

一个功能强大的MySQL数据库客户端工具，使用JavaFX开发，提供现代化的用户界面和丰富的功能。

## 功能特性

### 核心功能（MVP已实现）
- ✅ **连接管理**：支持多个MySQL连接配置，连接信息持久化保存
- ✅ **数据库浏览器**：树形结构展示数据库、表、列信息
- ✅ **SQL编辑器**：带语法高亮的SQL编辑器（基于RSyntaxTextArea）
- ✅ **查询执行**：执行SQL查询并显示结果
- ✅ **多标签支持**：支持多个SQL编辑器标签页
- ✅ **异步执行**：所有数据库操作在后台线程执行，UI不阻塞
- ✅ **连接池管理**：使用HikariCP管理数据库连接池

### 技术特性
- **密码加密**：使用Base64编码保存密码（⚠️ 注意：这不是安全的加密方式，仅用于简单混淆）
- **响应式设计**：支持窗口大小调整，可拖拽分割面板
- **错误处理**：友好的错误提示和详细的错误信息
- **状态栏**：实时显示连接状态和操作信息

## 技术栈

- **Java 17**
- **JavaFX 21**：现代化的UI框架
- **MySQL Connector/J 8.2.0**：MySQL JDBC驱动
- **HikariCP 5.1.0**：高性能连接池
- **RSyntaxTextArea 3.3.4**：SQL语法高亮
- **Jackson 2.16.0**：JSON序列化/反序列化
- **Maven**：项目构建和依赖管理

## 项目结构

```
mysql-client/
├── pom.xml                                 # Maven配置文件
├── README.md                               # 项目说明文档
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── mysqlclient/
        │           ├── MysqlClientApp.java              # 主应用程序入口
        │           ├── model/                           # 数据模型
        │           │   ├── ConnectionConfig.java        # 连接配置模型
        │           │   └── DatabaseNode.java            # 数据库树节点模型
        │           ├── service/                         # 业务逻辑层
        │           │   ├── ConnectionManager.java       # 连接管理器
        │           │   └── DatabaseService.java         # 数据库操作服务
        │           └── ui/                              # 用户界面层
        │               ├── MainWindow.java              # 主窗口
        │               ├── DatabaseTreeView.java        # 数据库树形视图
        │               ├── SqlEditorPane.java           # SQL编辑器面板
        │               └── StatusBar.java               # 状态栏
        └── resources/                                    # 资源文件
```

## 构建和运行

### 前置要求

- Java 17 或更高版本
- Maven 3.6 或更高版本
- MySQL 5.7+ 或 8.0+（用于连接测试）

### 构建项目

```bash
# 清理并编译
mvn clean compile

# 打包成可执行JAR（包含所有依赖）
mvn clean package
```

### 运行应用

#### 方式1：使用Maven运行
```bash
mvn javafx:run
```

#### 方式2：运行打包的JAR
```bash
java -jar target/mysql-client-1.0-SNAPSHOT.jar
```

### 打包说明

项目使用Maven Shade Plugin打包成fat JAR，包含所有依赖库。生成的JAR文件位于：
```
target/mysql-client-1.0-SNAPSHOT.jar
```

## 使用指南

### 1. 创建新连接

1. 点击工具栏的 "New Connection" 按钮或菜单 File -> New Connection
2. 填写连接信息：
   - Connection Name：连接名称（如：本地MySQL）
   - Host：主机地址（默认：localhost）
   - Port：端口号（默认：3306）
   - Username：用户名
   - Password：密码
3. 点击 "Connect" 按钮测试并保存连接

### 2. 浏览数据库

1. 在左侧数据库树中展开连接
2. 展开数据库查看表列表
3. 展开表查看列信息
4. 双击表名自动生成SELECT查询

### 3. 执行SQL查询

1. 点击 "New Query" 创建新的查询标签页
2. 在SQL编辑器中输入SQL语句
3. 点击 "Execute (F5)" 按钮或按F5键执行查询
4. 在下方的结果面板查看查询结果

### 4. 查看结果

- **Results标签**：显示查询返回的数据表格
- **Messages标签**：显示执行日志和错误信息
- 状态栏显示查询执行时间和影响行数

## 配置文件

连接配置保存在用户目录下：
```
~/.mysqlclient/connections.json
```

配置文件格式：
```json
[
  {
    "id": "uuid",
    "name": "本地MySQL",
    "host": "localhost",
    "port": 3306,
    "username": "root",
    "password": "base64EncodedPassword",
    "database": null,
    "useSSL": false
  }
]
```

## 性能优化

- **连接池配置**：
  - 最大连接数：10
  - 最小空闲连接：2
  - 连接超时：30秒
  - 查询超时：30秒

- **内存优化**：
  - 使用虚拟化表格显示大数据集
  - 后台线程执行数据库操作
  - 及时释放不用的连接

## 已知限制

1. **密码安全性**：密码使用Base64编码，不是安全的加密方式。在生产环境中应使用更安全的加密算法。
2. **大数据集**：当前版本对超大数据集（百万级）的显示性能可能不佳。
3. **数据编辑**：当前版本不支持直接在表格中编辑数据（计划在后续版本实现）。

## 后续计划功能

- [ ] 数据表格内编辑
- [ ] 数据导入导出（CSV, SQL）
- [ ] SQL执行历史记录
- [ ] 查询收藏夹
- [ ] 表结构设计器
- [ ] 数据备份和恢复
- [ ] 深色主题支持
- [ ] 多语言支持

## 开发说明

### 添加新功能

1. 在`model`包中定义数据模型
2. 在`service`包中实现业务逻辑
3. 在`ui`包中创建用户界面组件
4. 在`MainWindow`中集成新功能

### 代码风格

- 使用Java标准命名规范
- 所有数据库操作必须在后台线程执行
- UI更新必须在JavaFX Application Thread中执行
- 使用try-with-resources自动关闭资源

## 故障排查

### 构建失败

```bash
# 清理Maven本地仓库缓存
mvn dependency:purge-local-repository

# 重新构建
mvn clean package
```

### 无法连接数据库

1. 检查MySQL服务是否运行
2. 验证主机名、端口号、用户名和密码
3. 检查防火墙设置
4. 确认MySQL允许远程连接（如果不是localhost）

### JavaFX运行时错误

确保Java版本为17或更高，并且正确配置了JavaFX模块：
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/mysql-client-1.0-SNAPSHOT.jar
```

## 许可证

本项目仅用于学习和演示目的。

## 贡献

欢迎提交Issue和Pull Request！

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交Issue
- 发送邮件

---

**注意**：本应用使用Base64编码存储密码，这不是安全的加密方式。请不要在生产环境中使用，或者在使用前实现更安全的密码加密方案（如AES加密）。
