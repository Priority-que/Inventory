# Inventory 项目 Git 协作手册

## 1. 文档目的

本文档用于约定本项目的 Git 协作方式，适合两人小组日常开发使用。

目标是：

- 保证 `main` 分支始终稳定
- 减少两个人同时开发时的冲突
- 让每天的开发流程固定下来
- 让提交记录清晰，后续方便排错、回顾和写简历

本手册适用于当前仓库：

- [Inventory GitHub 仓库](https://github.com/Priority-que/Inventory)

## 2. 协作基本原则

### 2.1 主分支原则

- `main` 是稳定分支
- 不要直接在 `main` 上写业务功能
- 所有新功能都从 `main` 拉新分支开发

### 2.2 分支原则

- 一个功能一个分支
- 一个 bug 修复一个分支
- 一个文档调整一个分支
- 分支不要承担太多不相关内容

### 2.3 提交原则

- 小步提交
- 提交信息要能看懂
- 不要积攒太多修改后一次性提交
- 每次提交尽量只做一类事情

### 2.4 合并原则

- 先同步最新 `main`
- 再解决冲突
- 再推送功能分支
- 最后通过 GitHub 合并到 `main`

## 3. 仓库分支约定

当前建议使用如下分支：

- `main`：稳定主分支
- `feature/*`：新功能开发分支
- `fix/*`：缺陷修复分支
- `docs/*`：文档分支
- `refactor/*`：重构分支

## 4. 分支命名规范

### 4.1 功能分支

格式：

```bash
feature/模块-功能
```

示例：

```bash
feature/auth-login
feature/user-management
feature/material-crud
feature/purchase-request
feature/purchase-order
feature/inbound-order
feature/inventory-query
```

### 4.2 修复分支

格式：

```bash
fix/模块-问题
```

示例：

```bash
fix/login-token
fix/request-status
fix/inventory-calc
```

### 4.3 文档分支

格式：

```bash
docs/内容
```

示例：

```bash
docs/update-readme
docs/update-api-doc
```

## 5. 每天开发前的标准流程

每天开始开发前，先执行下面的流程：

### 第一步：切回主分支

```bash
git checkout main
```

### 第二步：拉取远程最新代码

```bash
git pull origin main
```

### 第三步：创建当天要开发的功能分支

```bash
git checkout -b feature/purchase-request
```

如果该分支之前已经创建过，不用重复创建，可以直接切换：

```bash
git checkout feature/purchase-request
```

## 6. 每天开发中的标准流程

### 6.1 开发过程中小步提交

开发一部分后就提交，不要等到所有代码全写完。

常用流程：

```bash
git add .
git commit -m "feat(purchase): add request create api"
```

继续开发：

```bash
git add .
git commit -m "feat(purchase): add request submit api"
```

### 6.2 定期推送远程备份

每天开发过程中建议至少推送一次，避免本地代码丢失。

第一次推送：

```bash
git push -u origin feature/purchase-request
```

后续继续推送：

```bash
git push
```

## 7. 每天开发结束前的标准流程

每天收工前建议做下面几件事：

### 第一步：检查改动

```bash
git status
```

### 第二步：把当天代码提交干净

```bash
git add .
git commit -m "feat(purchase): complete request basic flow"
```

### 第三步：推送到远程

```bash
git push
```

这样第二天继续开发时，不会丢失进度。

## 8. 功能完成后的标准流程

当某个功能基本写完，准备合并到 `main` 时，按下面流程走。

### 第一步：先切回主分支并拉最新代码

```bash
git checkout main
git pull origin main
```

### 第二步：回到自己的功能分支

```bash
git checkout feature/purchase-request
```

### 第三步：把最新 `main` 合并进来

```bash
git merge main
```

如果没有冲突，直接继续。

如果有冲突，修改冲突文件后执行：

```bash
git add .
git commit -m "merge: resolve conflicts with main"
```

### 第四步：推送分支

```bash
git push
```

### 第五步：在 GitHub 发起 Pull Request

让另一个同学看一下，再合并到 `main`。

## 9. 合并完成后的清理流程

功能分支已经成功合并到 `main` 后，可以删除本地和远程分支。

### 删除本地分支

```bash
git checkout main
git pull origin main
git branch -d feature/purchase-request
```

### 删除远程分支

```bash
git push origin --delete feature/purchase-request
```

## 10. 提交信息规范

建议统一采用下面格式：

```bash
类型(模块): 简短说明
```

### 10.1 常用类型

- `feat`：新增功能
- `fix`：修复问题
- `docs`：文档修改
- `refactor`：代码重构
- `test`：测试相关
- `chore`：杂项修改、工程配置修改

### 10.2 示例

```bash
feat(auth): add login api
feat(system): add user crud
feat(base): add material management
feat(purchase): add request submit flow
fix(purchase): fix request status check
fix(inventory): fix stock quantity update
docs(readme): update startup guide
chore(git): initialize repository
refactor(system): simplify role service
```

### 10.3 不建议的提交信息

下面这些提交信息尽量不要写：

```bash
update
修改一下
提交代码
test
最终版
```

因为以后回头看几乎不知道改了什么。

## 11. 两人开发的模块分工建议

为了减少冲突，建议先按模块固定分工。

### 同学 A 负责

- `auth`
- `purchase`
- `warehouse`
- `inventory`

### 同学 B 负责

- `system`
- `base`
- `supplier`
- `message`
- `log`
- `report`

这样可以尽量避免同时改同一批文件。

## 12. 冲突最少的开发方式

### 推荐做法

- 开发前先同步 `main`
- 每个功能单独分支
- 一个分支只做一个功能
- 频繁提交
- 合并前先和同学说一声

### 不推荐做法

- 两个人都直接在 `main` 上写
- 一个人一个分支里同时写很多不相关功能
- 很久不拉最新 `main`
- 改了公共文件但不提前沟通

## 13. 什么时候一定要先和同学沟通

遇到下面情况时，先说一声再改：

- 要改公共配置文件
- 要改数据库脚本
- 要改统一返回结构
- 要改全局异常处理
- 要改项目目录结构
- 要改状态枚举
- 要改权限设计

这些改动很容易影响对方正在写的代码。

## 14. 常见命令速查

### 查看当前状态

```bash
git status
```

### 查看当前分支

```bash
git branch
```

### 切换分支

```bash
git checkout main
git checkout feature/auth-login
```

### 创建并切换新分支

```bash
git checkout -b feature/auth-login
```

### 拉取主分支最新代码

```bash
git checkout main
git pull origin main
```

### 添加并提交

```bash
git add .
git commit -m "feat(auth): add login api"
```

### 推送分支

```bash
git push -u origin feature/auth-login
```

### 更新本地主分支后合并到当前功能分支

```bash
git checkout main
git pull origin main
git checkout feature/auth-login
git merge main
```

### 删除本地分支

```bash
git branch -d feature/auth-login
```

### 删除远程分支

```bash
git push origin --delete feature/auth-login
```

## 15. 适合你们项目的实际开发节奏

### 第一天

- 完成项目初始化
- 建库脚本入库
- 统一文档
- 统一模块划分
- 统一 Git 流程

### 后续每天

1. 先 `pull origin main`
2. 再切自己的功能分支
3. 完成功能的一小部分就提交
4. 每天结束前 push 到远程
5. 功能完成后再走 Pull Request 合并

## 16. 最后总结

你们现在最应该坚持的不是复杂流程，而是下面这 6 条：

1. 不直接在 `main` 上开发
2. 开发前先同步主分支
3. 一个功能一个分支
4. 小步提交
5. 提交信息写清楚
6. 合并前先同步再处理冲突

只要你们把这 6 条坚持住，后面两个人一起开发这个项目会顺很多。
