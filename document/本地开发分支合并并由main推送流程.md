# Inventory 本地开发分支合并并由 main 推送流程

## 1. 文档目的

本文档用于约定当前项目最适合的 Git 使用方式：

- 平时在本地功能分支开发
- 功能完成后先合并到本地 `main`
- 再由本地 `main` 推送到远程仓库
- 远程仓库长期只保留一条稳定可运行的 `main` 主线

这份流程特别适合当前项目阶段：

- 只有一个远程仓库
- 当前主要开发后端
- 远程希望尽量保持整洁
- 功能分支可以只在本地保留，远程按需临时存在

## 2. 适用原则

- `main` 是本地和远程的稳定主线。
- 新功能一律从本地 `main` 拉本地功能分支开发。
- 功能分支上可以反复提交。
- 只有功能基本可运行时，才合并到本地 `main`。
- 远程最终只保留 `main` 也没问题。
- 切分支前，尽量保证工作区干净。

## 3. 标准开发流程

### 3.1 从本地 `main` 创建功能分支

先切到本地 `main`：

```bash
git switch main
```

如果远程 `main` 有更新，先同步：

```bash
git pull origin main
```

然后创建并切换到新的本地功能分支：

```bash
git switch -c feature/material-crud
```

如果该分支已经存在，只需要切换：

```bash
git switch feature/material-crud
```

### 3.2 在功能分支开发和提交

开发完成一个小阶段后提交：

```bash
git add .
git commit -m "feat: 完成 material 基础 CRUD"
```

如果只想本地开发，不推远程，可以停在这里。

如果想临时备份到远程：

```bash
git push -u origin feature/material-crud
```

后续继续推送：

```bash
git push
```

## 4. 功能完成后合并到本地 `main`

### 4.1 确认当前功能分支是干净的

先看状态：

```bash
git status
```

理想状态应为：

```bash
nothing to commit, working tree clean
```

如果工作区不干净，有两种处理方式：

- 要保留这些改动：先 `git add` + `git commit`
- 暂时不想提交：先 `git stash push -u -m "wip xxx"`

### 4.2 切回本地 `main`

```bash
git switch main
```

### 4.3 将功能分支合并到本地 `main`

当前项目推荐优先使用快进合并：

```bash
git merge --ff-only feature/material-crud
```

如果看到：

```bash
Already up to date.
```

说明本地 `main` 已经和该功能分支在同一个提交上，不需要重复合并。

如果快进失败，说明 `main` 和功能分支都各自往前提交过。这时可以改用普通合并：

```bash
git merge feature/material-crud
```

如果出现冲突：

1. 手动解决冲突
2. 再执行：

```bash
git add .
git commit -m "merge: 解决 main 与 feature/material-crud 的冲突"
```

## 5. 由本地 `main` 推送到远程

合并成功后，直接推送本地 `main`：

```bash
git push origin main
```

这样远程 `main` 就会更新为当前可运行版本。

## 6. 远程功能分支的处理方式

### 6.1 如果远程功能分支只是临时备份

在远程 `main` 已经同步后，可以删除远程功能分支：

```bash
git push origin --delete feature/material-crud
```

### 6.2 如果本地功能分支还想保留

可以继续保留本地分支，不影响后续开发：

```bash
git branch
```

如果切回该分支时看到：

```bash
Your branch is based on 'origin/feature/material-crud', but the upstream is gone.
```

这不是错误，只是提示远程同名分支已经删掉了。

如果你不想再看到这条提示，可以取消本地分支的远程跟踪关系：

```bash
git branch --unset-upstream
```

### 6.3 如果本地功能分支也不再需要

可以在确认已经合并进 `main` 后删除：

```bash
git branch -d feature/material-crud
```

## 7. 常用完整示例

### 7.1 新开功能分支并开发

```bash
git switch main
git pull origin main
git switch -c feature/material-crud
git add .
git commit -m "feat: 完成 material 基础 CRUD"
```

### 7.2 将功能合并回本地 `main` 并推送远程

```bash
git switch main
git merge --ff-only feature/material-crud
git push origin main
```

### 7.3 删除远程功能分支，但保留本地分支

```bash
git push origin --delete feature/material-crud
git switch feature/material-crud
git branch --unset-upstream
```

### 7.4 删除远程和本地功能分支

```bash
git switch main
git push origin --delete feature/material-crud
git branch -d feature/material-crud
```

## 8. 工作区不干净时的处理

### 8.1 临时保存当前改动

```bash
git stash push -u -m "wip after material"
```

### 8.2 恢复暂存改动

```bash
git stash pop
```

如果 `stash pop` 时提示某些文件会被覆盖，通常说明当前工作区已经有同名改动。此时建议：

1. 先处理当前工作区改动
2. 再恢复 stash

### 8.3 查看 stash 列表

```bash
git stash list
```

## 9. 特别说明

- `.idea/` 建议写入 `.gitignore`，不要再让 IDE 本地文件进入仓库。
- 只要 `.git` 目录还在仓库根目录下，Git 管理的始终是整个仓库，而不是单独后端目录。
- 就算 IDEA 里重点展示的是 `inventory_back` 模块，也不会影响你提交整个仓库中的 `document`、后端代码和后续前端代码。
- 后续如果新增 `inventory_front/`，只要放在仓库根目录下，依然会被同一个仓库统一管理。

## 10. 推荐做法总结

最推荐的日常流程如下：

1. 从本地 `main` 创建本地功能分支
2. 在功能分支开发和提交
3. 功能完成后合并到本地 `main`
4. 由本地 `main` 推送到远程
5. 远程功能分支只作临时备份，需要时删除
6. 本地功能分支可按需要保留或删除

这套流程最适合当前项目：远程保持整洁，本地开发灵活，`main` 始终是一条可运行主线。
