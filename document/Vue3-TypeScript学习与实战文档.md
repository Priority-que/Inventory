# Vue3 + TypeScript 学习与实战文档

> 适合有 JavaScript 和 Vue3 基础，但 TypeScript 基础较薄弱的同学。本文不追求覆盖 TypeScript 全部理论，只围绕 Vue3 项目中最常用、最能落地的写法来讲。

## 阅读建议

刚开始不用一次性背完。建议按这个顺序学习：

1. 先看第一到四章，理解 TypeScript 的基本写法。
2. 再看第五章，重点掌握 Vue3 里 `ref`、`reactive`、`props`、`emits` 的写法。
3. 做项目时反复查第六、七、九章。
4. 最后照着第十一章完整案例练一遍。

## 先看懂几个 TypeScript 符号

如果你是从 JavaScript 切到 TypeScript，刚开始最容易卡住的不是大理论，而是这些符号：

| TS 写法 | 读法 | 含义 |
| --- | --- | --- |
| `name: string` | name 是 string 类型 | 变量或字段必须是字符串 |
| `age: number` | age 是 number 类型 | 变量或字段必须是数字 |
| `loading: boolean` | loading 是 boolean 类型 | 变量或字段必须是布尔值 |
| `nickname?: string` | nickname 是可选 string 类型 | 这个字段可以有，也可以没有 |
| `id: number \| string` | id 可以是 number 或 string | 联合类型，允许多个类型 |
| `Book[]` | Book 数组 | 表示数组里每一项都是 Book |
| `Array<Book>` | Book 数组 | 和 `Book[]` 含义一样 |
| `Promise<Book[]>` | Promise 最终返回 Book 数组 | 常用于接口请求 |
| `Result<Book>` | Result 里的 data 是 Book | 泛型，常用于后端统一返回 |

### 重点：`?` 不是 nickname 专属

你提到的 `nickname?` 很关键。这里的重点不是 `nickname` 这个名字，而是字段名后面的 `?`。

只要某个字段后面加了 `?`，它就是可选字段。

比如这些都可以：

```ts
interface User {
  id: number
  username: string
  nickname?: string
}

interface Book {
  id: number
  bookName?: string
  author?: string
}

interface QueryParams {
  pageNum: number
  pageSize: number
  keyword?: string
}
```

上面三个例子里：

- `nickname?` 表示用户昵称可以没有。
- `bookName?` 表示图书名称可以没有。
- `author?` 表示作者可以没有。
- `keyword?` 表示查询关键字可以没有。

所以 `?` 的规则是：

```ts
字段名?: 类型
```

它表示：

```text
这个字段可以存在，存在时必须符合指定类型；
这个字段也可以不存在。
```

### JS 和 TS 对比：可选字段

JavaScript 里对象字段天生可以缺失：

```js
const user = {
  id: 1,
  username: 'zhangsan'
}

console.log(user.nickname) // undefined，不报错
```

JS 不会提前告诉你 `nickname` 到底该不该有、是什么类型。

TypeScript 里你要把这个规则写清楚：

```ts
interface User {
  id: number
  username: string
  nickname?: string
}

const user: User = {
  id: 1,
  username: 'zhangsan'
}
```

因为 `nickname` 是可选字段，所以不写也没问题。

但是如果写了，就必须是字符串：

```ts
const user: User = {
  id: 1,
  username: 'zhangsan',
  nickname: '小张'
}
```

下面这样会报错：

```ts
const user: User = {
  id: 1,
  username: 'zhangsan',
  nickname: 123 // 报错：nickname 如果存在，必须是 string
}
```

### `?` 和 `| undefined` 的关系

下面两种写法很接近：

```ts
interface UserA {
  nickname?: string
}
```

```ts
interface UserB {
  nickname: string | undefined
}
```

但实际项目里更常用第一种：

```ts
nickname?: string
```

因为它表达的是“这个字段可以不传”。

而下面这种：

```ts
nickname: string | undefined
```

更像是“这个字段必须写出来，只是值可以是 undefined”。

例如：

```ts
const userA: UserA = {}
```

可以。

```ts
const userB: UserB = {}
```

会报错，因为 `nickname` 字段本身没有写。

### 初学者可以先记住这几个规律

```ts
name: string
```

表示必填字符串字段。

```ts
name?: string
```

表示可选字符串字段。

```ts
name: string | null
```

表示字段必须存在，但值可以是字符串或 null。

```ts
name?: string | null
```

表示字段可以不存在；如果存在，可以是字符串或 null。

在 Vue 项目里最常见的是：

```ts
interface BookQuery {
  pageNum: number
  pageSize: number
  keyword?: string
}
```

因为查询条件里的关键字通常不是必填。

---

## 一、TypeScript 和 JavaScript 的区别

### 1. TypeScript 是什么

TypeScript，简称 TS，可以理解为“带类型的 JavaScript”。

JavaScript 是动态类型语言：

```js
let age = 18
age = '十八'
```

这段 JS 可以运行，但如果业务里 `age` 本来应该是数字，后面改成字符串，就可能在运行时出问题。

TypeScript 会在开发阶段提前提醒你：

```ts
let age: number = 18
age = '十八' // 报错：string 不能赋值给 number
```

TypeScript 最重要的作用不是让代码“看起来高级”，而是让你在写代码时提前发现低级错误。

### 2. 为什么 Vue 项目中越来越多使用 TypeScript

Vue 后台管理项目里常见这些场景：

- 表单字段很多。
- 表格数据来自后端。
- 接口参数和返回值结构复杂。
- 页面之间、父子组件之间要传数据。
- 采购单、入库单、供应商、用户等业务对象字段多。

如果全用 JS，很多错误只有点开页面、调用接口后才发现。

使用 TS 后，可以提前发现：

- 字段名写错。
- 参数类型传错。
- 接口返回值使用错误。
- 表单对象缺字段。
- `ref(null)` 后直接调用方法导致空值错误。

### 3. TypeScript 相比 JavaScript 的优势

| 对比点 | JavaScript | TypeScript |
| --- | --- | --- |
| 类型检查 | 运行时才容易发现问题 | 开发阶段提前提示 |
| 代码提示 | 依赖编辑器猜测 | 根据类型准确提示 |
| 接口字段 | 容易写错字段名 | 字段名有自动补全 |
| 重构 | 改字段容易漏 | 改类型后多处报错提醒 |
| 学习成本 | 低 | 稍高，但很值得 |

### 4. TypeScript 是否会影响运行时

不会。

浏览器不能直接运行 TypeScript。Vue 项目在打包时会把 TS 转成 JS，最终运行的还是 JS。

也就是说：

- TS 是开发阶段的工具。
- TS 的类型不会出现在浏览器运行时代码里。
- 类型写错会影响编译，但不会变成运行时代码。

### 5. 简单对比 JS 写法和 TS 写法

JS：

```js
function getUserName(user) {
  return user.name
}
```

问题：你不知道 `user` 到底应该有什么字段。

TS：

```ts
interface User {
  id: number
  name: string
}

function getUserName(user: User): string {
  return user.name
}
```

作用：

- `user` 必须有 `id` 和 `name`。
- `name` 必须是字符串。
- 函数返回值必须是字符串。

### 6. 常见 JS 写法改成 TS 写法

这一节可以当成“翻译表”来看。你看到 TS 代码时，可以先按 JS 的习惯理解，再看它多出来的类型约束。

#### 变量

JS：

```js
let bookName = 'Vue3 入门'
```

TS：

```ts
let bookName: string = 'Vue3 入门'
```

区别：

- JS 只保存值。
- TS 既保存值，也声明这个变量以后应该是什么类型。

#### 数字

JS：

```js
let price = 99
```

TS：

```ts
let price: number = 99
```

如果你后面写：

```ts
price = '99'
```

TS 会报错，因为字符串 `'99'` 不是数字 `99`。

#### 布尔值

JS：

```js
let loading = false
```

TS：

```ts
let loading: boolean = false
```

项目中常用于：

```ts
const loading = ref<boolean>(false)
const dialogVisible = ref<boolean>(false)
```

#### 数组

JS：

```js
const list = []
list.push({ id: 1, name: 'Vue3 入门' })
```

TS：

```ts
interface Book {
  id: number
  name: string
}

const list: Book[] = []
list.push({ id: 1, name: 'Vue3 入门' })
```

如果你写：

```ts
list.push({ id: 2 })
```

TS 会报错，因为缺少 `name`。

#### 对象

JS：

```js
const form = {
  name: '',
  price: 0
}
```

TS：

```ts
interface BookForm {
  name: string
  price: number
}

const form: BookForm = {
  name: '',
  price: 0
}
```

区别：

- JS 只看对象现在有什么字段。
- TS 会要求对象符合 `BookForm` 这套规则。

#### 函数参数

JS：

```js
function deleteBook(id) {
  console.log(id)
}
```

TS：

```ts
function deleteBook(id: number): void {
  console.log(id)
}
```

区别：

- `id: number` 表示调用时必须传数字。
- `: void` 表示这个函数没有返回值。

#### 函数返回值

JS：

```js
function getStatusText(status) {
  return status === 'normal' ? '正常' : '停用'
}
```

TS：

```ts
type Status = 'normal' | 'disabled'

function getStatusText(status: Status): string {
  return status === 'normal' ? '正常' : '停用'
}
```

区别：

- `status` 不能乱传，只能是 `normal` 或 `disabled`。
- 函数必须返回字符串。

#### 接口请求

JS：

```js
async function getBookList(params) {
  return request.get('/books/page', { params })
}
```

TS：

```ts
interface BookQuery {
  pageNum: number
  pageSize: number
  keyword?: string
}

interface Book {
  id: number
  name: string
}

interface Result<T> {
  code: number
  msg: string
  data: T
}

interface PageResult<T> {
  records: T[]
  total: number
}

function getBookList(params: BookQuery): Promise<Result<PageResult<Book>>> {
  return request.get('/books/page', { params })
}
```

这段 TS 的意思是：

- 请求参数必须符合 `BookQuery`。
- 返回结果是一个 Promise。
- Promise 里面是 `Result<PageResult<Book>>`。
- 最终 `res.data.records` 是 `Book[]`。

#### Vue ref

JS：

```js
const tableData = ref([])
```

TS：

```ts
const tableData = ref<Book[]>([])
```

区别：

- JS 只知道这是数组。
- TS 知道这是图书数组，数组里每一项都是 `Book`。

#### Vue reactive

JS：

```js
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})
```

TS：

```ts
interface BookQuery {
  pageNum: number
  pageSize: number
  keyword?: string
}

const queryParams = reactive<BookQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})
```

区别：

- TS 会检查 `pageNum`、`pageSize` 是否是数字。
- `keyword` 是可选字段，可以有，也可以没有。

---

## 二、TypeScript 基础类型

### 1. string

JS 写法：

```js
let name = '张三'
```

TS 写法：

```ts
let name: string = '张三'
```

项目中什么时候用：

- 用户名
- 物料名称
- 图书名称
- 手机号
- 状态文本
- 日期字符串

示例：

```ts
const bookName: string = 'Vue3 入门'
```

### 2. number

JS 写法：

```js
let price = 99.8
```

TS 写法：

```ts
let price: number = 99.8
```

项目中什么时候用：

- 主键 id
- 数量
- 价格
- 页码
- 总数

示例：

```ts
const currentPage: number = 1
const pageSize: number = 10
```

### 3. boolean

JS 写法：

```js
let loading = false
```

TS 写法：

```ts
let loading: boolean = false
```

项目中什么时候用：

- 加载状态
- 弹窗显示隐藏
- 是否启用
- 是否选中

示例：

```ts
const dialogVisible: boolean = false
```

### 4. null

`null` 表示“现在没有值”。

JS 写法：

```js
let currentRow = null
```

TS 写法：

```ts
interface Book {
  id: number
  name: string
}

let currentRow: Book | null = null
```

项目中什么时候用：

- 当前选中的表格行，初始时还没有选中。
- 编辑弹窗，初始时没有编辑对象。
- 父组件调用子组件实例，初始时组件还没挂载。

### 5. undefined

`undefined` 表示“没有被赋值”。

JS 写法：

```js
let keyword
```

TS 写法：

```ts
let keyword: string | undefined
```

项目中什么时候用：

- 可选参数。
- 后端可能不返回某个字段。
- 表单中某些字段不是必填。

示例：

```ts
interface QueryParams {
  keyword?: string
}
```

这里的 `keyword?: string` 等价于 `keyword: string | undefined`。

### 6. any

`any` 表示“什么类型都可以”，它会关闭 TypeScript 对这个值的检查。

JS 写法本质上接近 `any`：

```js
let data = {}
data = 'hello'
data = 123
```

TS 写法：

```ts
let data: any = {}
data = 'hello'
data = 123
```

项目中什么时候可以用：

- 临时接第三方库，类型不好写。
- 后端返回结构暂时不确定。
- 刚从 JS 迁移到 TS，先让项目跑起来。

不建议：

```ts
const list: any[] = []
```

更建议：

```ts
interface Book {
  id: number
  name: string
}

const list: Book[] = []
```

### 7. unknown

`unknown` 也表示未知类型，但比 `any` 安全。

```ts
let value: unknown = 'hello'
```

不能直接调用：

```ts
value.toUpperCase() // 报错
```

需要先判断：

```ts
if (typeof value === 'string') {
  value.toUpperCase()
}
```

项目中什么时候用：

- `catch` 捕获错误。
- 不确定外部传来的数据结构。

示例：

```ts
try {
  // 请求接口
} catch (error: unknown) {
  if (error instanceof Error) {
    console.log(error.message)
  }
}
```

### 8. void

`void` 表示函数没有返回值。

JS：

```js
function showMessage() {
  console.log('保存成功')
}
```

TS：

```ts
function showMessage(): void {
  console.log('保存成功')
}
```

项目中什么时候用：

- 点击按钮。
- 打开弹窗。
- 重置表单。
- 只执行操作但不返回数据的函数。

### 9. never

`never` 表示永远不会正常返回。

常见场景不多，初学者知道即可。

```ts
function throwError(message: string): never {
  throw new Error(message)
}
```

项目中什么时候用：

- 抛出异常的工具函数。
- 判断状态时做兜底检查。

普通 Vue 页面开发里很少主动写 `never`。

### 10. 数组类型

JS：

```js
const names = ['张三', '李四']
```

TS 写法一：

```ts
const names: string[] = ['张三', '李四']
```

TS 写法二：

```ts
const names: Array<string> = ['张三', '李四']
```

对象数组：

```ts
interface Book {
  id: number
  name: string
}

const bookList: Book[] = [
  { id: 1, name: 'Vue3 入门' }
]
```

项目中什么时候用：

- 表格数据。
- 下拉选项。
- 多选结果。
- 菜单列表。

### 11. 对象类型

JS：

```js
const user = {
  id: 1,
  name: '张三'
}
```

TS 简单写法：

```ts
const user: { id: number; name: string } = {
  id: 1,
  name: '张三'
}
```

更推荐使用 `interface`：

```ts
interface User {
  id: number
  name: string
}

const user: User = {
  id: 1,
  name: '张三'
}
```

项目中什么时候用：

- 表单对象。
- 当前登录用户。
- 表格一行数据。
- 后端返回对象。

### 12. 联合类型

联合类型表示“可以是多种类型中的一种”。

```ts
let id: number | string = 1
id = '001'
```

项目中什么时候用：

- id 可能是数字，也可能是字符串。
- 表格当前行可能是对象，也可能是 null。
- 日期范围可能是数组，也可能为空。

示例：

```ts
interface Book {
  id: number
  name: string
}

const currentBook = ref<Book | null>(null)
```

### 13. 字面量类型

字面量类型表示值只能是固定几个。

```ts
type Status = 'draft' | 'published' | 'disabled'

let status: Status = 'draft'
status = 'published'
status = 'unknown' // 报错
```

项目中什么时候用：

- 订单状态。
- 审核状态。
- 弹窗模式。
- 用户角色。

示例：

```ts
type DialogMode = 'add' | 'edit'

const mode = ref<DialogMode>('add')
```

### 14. 类型推断

TypeScript 很多时候能自己推断类型。

```ts
const name = '张三' // 推断为 string
const age = 18 // 推断为 number
const loading = false // 推断为 boolean
```

不一定所有变量都要手写类型。

推荐规则：

- 简单变量可以让 TS 自动推断。
- 接口返回值、表单对象、数组、props、emits 要明确写类型。

---

## 三、函数中的 TypeScript 用法

### 1. 函数参数类型

JS：

```js
function getBookDetail(id) {
  console.log(id)
}
```

TS：

```ts
function getBookDetail(id: number): void {
  console.log(id)
}
```

项目场景：根据 id 查询详情。

### 2. 函数返回值类型

```ts
function getTotal(price: number, count: number): number {
  return price * count
}
```

如果返回字符串：

```ts
function getStatusText(status: string): string {
  return status === 'enabled' ? '启用' : '停用'
}
```

### 3. 可选参数

```ts
function searchBook(keyword?: string): void {
  console.log(keyword)
}
```

`keyword?: string` 表示可以传，也可以不传。

项目场景：查询条件不是必填。

### 4. 默认参数

```ts
function queryList(page: number = 1, size: number = 10): void {
  console.log(page, size)
}
```

项目场景：分页查询默认第一页、每页 10 条。

### 5. 箭头函数的类型写法

常见写法：

```ts
const deleteBook = (id: number): void => {
  console.log('删除图书', id)
}
```

也可以先定义函数类型：

```ts
type DeleteHandler = (id: number) => void

const deleteBook: DeleteHandler = (id) => {
  console.log(id)
}
```

初学阶段更推荐第一种，直观。

### 6. void 返回值

按钮点击函数通常返回 `void`：

```ts
const openDialog = (): void => {
  dialogVisible.value = true
}
```

### 7. Promise 返回值类型

接口请求通常返回 Promise。

```ts
interface Book {
  id: number
  name: string
}

async function getBookList(): Promise<Book[]> {
  const list: Book[] = []
  return list
}
```

项目场景：查询列表函数。

```ts
interface QueryParams {
  pageNum: number
  pageSize: number
  keyword?: string
}

interface Book {
  id: number
  name: string
}

const queryBookList = async (params: QueryParams): Promise<Book[]> => {
  console.log(params)
  return []
}
```

删除数据函数：

```ts
const removeBook = async (id: number): Promise<void> => {
  console.log('请求删除', id)
}
```

提交表单函数：

```ts
interface BookForm {
  name: string
  author: string
  price: number
}

const submitBookForm = async (form: BookForm): Promise<void> => {
  console.log('提交表单', form)
}
```

接口请求函数：

```ts
interface Result<T> {
  code: number
  msg: string
  data: T
}

async function requestBookDetail(id: number): Promise<Result<Book>> {
  return {
    code: 200,
    msg: 'success',
    data: { id, name: 'Vue3 入门' }
  }
}
```

---

## 四、interface 和 type 的常见用法

### 1. interface 是什么

`interface` 通常用来描述对象结构。

JavaScript 写对象时，一般直接写：

```js
const user = {
  id: 1,
  username: 'admin',
  nickname: '管理员'
}
```

这在 JS 中完全没问题，但缺点是：别人看函数参数时，不一定知道这个对象应该有哪些字段。

比如：

```js
function showUser(user) {
  console.log(user.username)
  console.log(user.nickname)
}
```

这里的 `user` 到底应该长什么样？JS 不会告诉你。

TypeScript 的思路是：先把对象结构说清楚。

```ts
interface User {
  id: number
  username: string
  nickname?: string
}
```

这段代码不是创建对象，而是在定义“对象规则”。

它的意思是：

- `User` 类型的对象必须有 `id`。
- `id` 必须是 `number`。
- `User` 类型的对象必须有 `username`。
- `username` 必须是 `string`。
- `nickname` 是可选字段，可以有，也可以没有。
- 如果 `nickname` 存在，它必须是 `string`。

然后你可以这样使用：

```ts
const user1: User = {
  id: 1,
  username: 'admin'
}

const user2: User = {
  id: 2,
  username: 'lisi',
  nickname: '李四'
}
```

这两个都正确，因为 `nickname` 可以不写。

下面这样会报错：

```ts
const user3: User = {
  id: 3
  // 报错：缺少 username
}
```

下面这样也会报错：

```ts
const user4: User = {
  id: 4,
  username: 'wangwu',
  nickname: 123 // 报错：nickname 必须是 string
}
```

再次强调：`?` 可以用在任何字段名后面，不是只能写 `nickname?`。

```ts
interface Book {
  id: number
  bookName?: string
  author?: string
  price?: number
}
```

这里的 `bookName?`、`author?`、`price?` 都表示可选字段。

项目中常用来定义：

- 用户对象
- 表单对象
- 表格行数据
- 后端返回对象
- 组件 props

### 2. type 是什么

`type` 可以给任何类型起名字。

JavaScript 里没有 `type` 这个概念。你通常会直接写字符串：

```js
let status = 'normal'
status = 'disabled'
status = 'abc'
```

JS 不会阻止你把 `status` 改成 `abc`。

TypeScript 可以把允许的值固定下来：

```ts
type Status = 'enabled' | 'disabled'
```

这表示 `Status` 只能是 `'enabled'` 或 `'disabled'`。

```ts
let status: Status = 'enabled'
status = 'disabled'
status = 'abc' // 报错
```

也可以定义对象：

```ts
type Book = {
  id: number
  name: string
}
```

但是在项目里，普通对象结构更推荐用 `interface`。

### 3. interface 和 type 的区别

初学阶段不用纠结太深，记住这个简单规则：

- 描述对象结构，优先用 `interface`。
- 描述联合类型、字面量类型、函数类型，优先用 `type`。

示例：

```ts
interface Book {
  id: number
  name: string
}

type BookStatus = 'on_sale' | 'off_sale'
```

再看一个更贴近项目的对比：

JS 写法：

```js
const book = {
  id: 1,
  name: 'Vue3 入门',
  status: 'normal'
}
```

TS 写法：

```ts
type BookStatus = 'normal' | 'disabled'

interface Book {
  id: number
  name: string
  status: BookStatus
}

const book: Book = {
  id: 1,
  name: 'Vue3 入门',
  status: 'normal'
}
```

这样写的好处是：`status` 只能填 `normal` 或 `disabled`，不能随便填其他字符串。

### 4. 什么时候用 interface

```ts
interface User {
  id: number
  username: string
  roleName: string
}

interface Book {
  id: number
  name: string
  author: string
  price: number
}

interface Notice {
  id: number
  title: string
  content: string
  createTime: string
}
```

这些都是典型对象结构，适合用 `interface`。

### 5. 什么时候用 type

状态：

```ts
type BookStatus = 'normal' | 'disabled'
```

弹窗模式：

```ts
type DialogMode = 'add' | 'edit'
```

函数类型：

```ts
type SubmitHandler = () => Promise<void>
```

### 6. 定义后端返回的数据结构

通用返回：

```ts
export interface Result<T> {
  code: number
  msg: string
  data: T
}
```

分页返回：

```ts
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}
```

使用：

```ts
type BookPageResult = Result<PageResult<Book>>
```

### 7. 定义表单对象

```ts
export interface BookForm {
  id?: number
  name: string
  author: string
  price: number
  stock: number
}
```

`id?` 的原因：

- 新增时没有 id。
- 编辑时有 id。

### 8. 定义列表数据类型

```ts
export interface Book {
  id: number
  name: string
  author: string
  price: number
  stock: number
  status: 'normal' | 'disabled'
  createTime: string
}
```

### 9. 分页查询参数类型

```ts
export interface BookQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: string
}
```

---

## 五、Vue3 + TypeScript 常见写法

下面示例都使用：

```vue
<script setup lang="ts">
</script>
```

### 1. ref 的类型写法

#### ref<string>()

```vue
<script setup lang="ts">
import { ref } from 'vue'

const keyword = ref<string>('')
</script>
```

说明：查询关键字、输入框内容通常是字符串。

#### ref<number>()

```vue
<script setup lang="ts">
import { ref } from 'vue'

const total = ref<number>(0)
const currentPage = ref<number>(1)
</script>
```

说明：分页总数、页码通常是数字。

#### ref<boolean>()

```vue
<script setup lang="ts">
import { ref } from 'vue'

const loading = ref<boolean>(false)
const dialogVisible = ref<boolean>(false)
</script>
```

说明：加载状态、弹窗显示隐藏通常是布尔值。

#### ref<Array<T>>()

```vue
<script setup lang="ts">
import { ref } from 'vue'

interface Book {
  id: number
  name: string
}

const bookList = ref<Book[]>([])
</script>
```

也可以写：

```ts
const bookList = ref<Array<Book>>([])
```

更推荐 `Book[]`，简洁。

#### ref<T | null>()

```vue
<script setup lang="ts">
import { ref } from 'vue'

interface Book {
  id: number
  name: string
}

const currentBook = ref<Book | null>(null)
</script>
```

说明：当前选中图书一开始为空，所以要加 `null`。

### 2. reactive 的类型写法

#### reactive 定义表单对象

```vue
<script setup lang="ts">
import { reactive } from 'vue'

interface BookForm {
  id?: number
  name: string
  author: string
  price: number
}

const form = reactive<BookForm>({
  name: '',
  author: '',
  price: 0
})
</script>
```

#### reactive 定义查询条件

```vue
<script setup lang="ts">
import { reactive } from 'vue'

interface BookQuery {
  pageNum: number
  pageSize: number
  keyword?: string
}

const queryParams = reactive<BookQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})
</script>
```

#### reactive 和 interface 配合使用

Vue 官方更推荐用这种方式：先定义 interface，再让变量符合这个 interface。

```ts
interface UserForm {
  username: string
  password: string
}

const loginForm: UserForm = reactive({
  username: '',
  password: ''
})
```

### 3. computed 的类型写法

#### computed 自动推断

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'

const price = ref(20)
const count = ref(3)

const totalPrice = computed(() => {
  return price.value * count.value
})
</script>
```

`totalPrice` 会自动推断为数字类型。

#### 手动指定 computed 类型

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'

const status = ref<'normal' | 'disabled'>('normal')

const statusText = computed<string>(() => {
  return status.value === 'normal' ? '正常' : '停用'
})
</script>
```

### 4. watch 的类型写法

#### watch 单个值

```vue
<script setup lang="ts">
import { ref, watch } from 'vue'

const keyword = ref<string>('')

watch(keyword, (newValue, oldValue) => {
  console.log('新值', newValue)
  console.log('旧值', oldValue)
})
</script>
```

#### watch 多个值

```vue
<script setup lang="ts">
import { ref, watch } from 'vue'

const pageNum = ref<number>(1)
const pageSize = ref<number>(10)

watch([pageNum, pageSize], ([newPageNum, newPageSize]) => {
  console.log(newPageNum, newPageSize)
})
</script>
```

#### watch 对象属性

```vue
<script setup lang="ts">
import { reactive, watch } from 'vue'

const queryParams = reactive({
  keyword: '',
  status: ''
})

watch(
  () => queryParams.keyword,
  (newValue) => {
    console.log('关键字变化', newValue)
  }
)
</script>
```

#### 监听日期范围并赋值 begin/end

```vue
<script setup lang="ts">
import { reactive, ref, watch } from 'vue'

const dateRange = ref<[string, string] | null>(null)

const queryParams = reactive({
  beginTime: '',
  endTime: ''
})

watch(dateRange, (value) => {
  if (value) {
    queryParams.beginTime = value[0]
    queryParams.endTime = value[1]
  } else {
    queryParams.beginTime = ''
    queryParams.endTime = ''
  }
})
</script>
```

### 5. defineProps 的类型写法

#### 普通 props

```vue
<script setup lang="ts">
const props = defineProps<{
  title: string
  count: number
}>()
</script>
```

#### 可选 props

```vue
<script setup lang="ts">
const props = defineProps<{
  title: string
  description?: string
}>()
</script>
```

#### 默认值 withDefaults

```vue
<script setup lang="ts">
interface Props {
  title: string
  pageSize?: number
}

const props = withDefaults(defineProps<Props>(), {
  pageSize: 10
})
</script>
```

#### interface 定义 props

```vue
<script setup lang="ts">
interface Book {
  id: number
  name: string
}

interface Props {
  book: Book
  readonly?: boolean
}

const props = defineProps<Props>()
</script>
```

### 6. defineEmits 的类型写法

#### 子组件向父组件传值

子组件：

```vue
<script setup lang="ts">
interface Book {
  id: number
  name: string
}

const emit = defineEmits<{
  select: [book: Book]
  close: []
}>()

const handleSelect = (book: Book): void => {
  emit('select', book)
}

const handleClose = (): void => {
  emit('close')
}
</script>
```

父组件使用：

```vue
<template>
  <BookList @select="handleSelectBook" @close="handleClose" />
</template>

<script setup lang="ts">
interface Book {
  id: number
  name: string
}

const handleSelectBook = (book: Book): void => {
  console.log(book)
}

const handleClose = (): void => {
  console.log('关闭')
}
</script>
```

### 7. defineExpose 的用法

子组件暴露方法：

```vue
<!-- BookFormDialog.vue -->
<script setup lang="ts">
import { ref } from 'vue'

const dialogVisible = ref(false)

const open = (): void => {
  dialogVisible.value = true
}

const close = (): void => {
  dialogVisible.value = false
}

defineExpose({
  open,
  close
})
</script>
```

父组件调用：

```vue
<template>
  <BookFormDialog ref="bookFormDialogRef" />
  <el-button type="primary" @click="openDialog">新增</el-button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BookFormDialog from './BookFormDialog.vue'

const bookFormDialogRef = ref<InstanceType<typeof BookFormDialog> | null>(null)

const openDialog = (): void => {
  bookFormDialogRef.value?.open()
}
</script>
```

说明：`?.` 表示如果子组件实例存在，就调用 `open`；如果还不存在，就不调用，避免空值错误。

---

## 六、Vue 项目中接口请求的 TypeScript 写法

### 1. 定义后端返回 Result<T>

```ts
export interface Result<T> {
  code: number
  msg: string
  data: T
}
```

`T` 是泛型，可以理解为“这里先占个位置，调用时再告诉它具体是什么类型”。

例如：

```ts
Result<Book>
Result<Book[]>
Result<PageResult<Book>>
```

### 2. 定义分页返回 PageResult<T>

```ts
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}
```

### 3. 定义请求参数类型

```ts
export interface BookQuery {
  pageNum: number
  pageSize: number
  keyword?: string
}
```

### 4. 定义返回数据类型

```ts
export interface Book {
  id: number
  name: string
  author: string
  price: number
  stock: number
}
```

### 5. get 请求如何写类型

```ts
import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type { Book, BookQuery } from '@/types/book'

export const getBookPage = (params: BookQuery): Promise<Result<PageResult<Book>>> => {
  return request.get('/books/page', { params })
}
```

### 6. post 请求如何写类型

```ts
import request from '@/utils/request'
import type { Result } from '@/types/common'
import type { BookForm } from '@/types/book'

export const addBook = (data: BookForm): Promise<Result<null>> => {
  return request.post('/books', data)
}
```

### 7. async/await 中如何写类型

```ts
const loadBookList = async (): Promise<void> => {
  const res = await getBookPage(queryParams)
  tableData.value = res.data.records
  total.value = res.data.total
}
```

### 8. 完整示例：types/book.ts

```ts
export interface Result<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export type BookStatus = 'normal' | 'disabled'

export interface Book {
  id: number
  name: string
  author: string
  price: number
  stock: number
  status: BookStatus
  createTime: string
}

export interface BookQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: BookStatus | ''
}

export interface BookForm {
  id?: number
  name: string
  author: string
  price: number
  stock: number
  status: BookStatus
}
```

作用：集中定义图书模块会用到的类型。

### 9. 完整示例：api/book.ts

```ts
import request from '@/utils/request'
import type { Book, BookForm, BookQuery, PageResult, Result } from '@/types/book'

export const getBookPage = (params: BookQuery): Promise<Result<PageResult<Book>>> => {
  return request.get('/books/page', { params })
}

export const addBook = (data: BookForm): Promise<Result<null>> => {
  return request.post('/books', data)
}

export const updateBook = (data: BookForm): Promise<Result<null>> => {
  return request.put(`/books/${data.id}`, data)
}

export const deleteBook = (id: number): Promise<Result<null>> => {
  return request.delete(`/books/${id}`)
}
```

作用：页面不直接写请求地址，而是通过 API 文件调用。

### 10. 在 Vue 页面中调用接口并渲染表格

```vue
<template>
  <el-table :data="tableData" v-loading="loading">
    <el-table-column prop="name" label="图书名称" />
    <el-table-column prop="author" label="作者" />
    <el-table-column prop="price" label="价格" />
  </el-table>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getBookPage } from '@/api/book'
import type { Book, BookQuery } from '@/types/book'

const loading = ref(false)
const tableData = ref<Book[]>([])

const queryParams = reactive<BookQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const loadData = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getBookPage(queryParams)
    tableData.value = res.data.records
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
```

作用：展示分页查询返回的图书列表。

---

## 七、Element Plus + TypeScript 常见用法

### 1. el-form 表单类型

常用类型：

```ts
import type { FormInstance, FormRules } from 'element-plus'
```

表单对象：

```ts
interface BookForm {
  name: string
  author: string
  price: number
}

const form = reactive<BookForm>({
  name: '',
  author: '',
  price: 0
})
```

表单校验规则：

```ts
const rules = reactive<FormRules<BookForm>>({
  name: [{ required: true, message: '请输入图书名称', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
})
```

formRef：

```ts
const formRef = ref<FormInstance>()
```

validate 调用：

```ts
const submitForm = async (): Promise<void> => {
  if (!formRef.value) return

  await formRef.value.validate()
  console.log('校验通过')
}
```

### 2. el-table 表格类型

```ts
interface Book {
  id: number
  name: string
}

const tableData = ref<Book[]>([])
const selectedRows = ref<Book[]>([])

const handleSelectionChange = (rows: Book[]): void => {
  selectedRows.value = rows
}

const currentRow = ref<Book | null>(null)

const handleCurrentChange = (row: Book | undefined): void => {
  currentRow.value = row || null
}
```

### 3. el-dialog 弹窗

```ts
type DialogMode = 'add' | 'edit'

const dialogVisible = ref<boolean>(false)
const dialogMode = ref<DialogMode>('add')

const openAddDialog = (): void => {
  dialogMode.value = 'add'
  dialogVisible.value = true
}

const openEditDialog = (row: Book): void => {
  dialogMode.value = 'edit'
  Object.assign(form, row)
  dialogVisible.value = true
}
```

### 4. el-pagination 分页

```ts
const currentPage = ref<number>(1)
const pageSize = ref<number>(10)
const total = ref<number>(0)

const handlePageChange = (page: number): void => {
  currentPage.value = page
  queryParams.pageNum = page
  loadData()
}

const handleSizeChange = (size: number): void => {
  pageSize.value = size
  queryParams.pageSize = size
  queryParams.pageNum = 1
  loadData()
}
```

### 5. Element Plus 页面结构示例

```vue
<template>
  <div class="book-page">
    <el-form :model="queryParams" inline>
      <el-form-item label="关键字">
        <el-input v-model="queryParams.keyword" placeholder="请输入图书名称" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openAddDialog">新增</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="图书名称" />
      <el-table-column prop="author" label="作者" />
      <el-table-column prop="price" label="价格" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="loadData"
      @size-change="loadData"
    />
  </div>
</template>
```

作用：展示一个列表页的基本结构：筛选、工具栏、表格、分页。

---

## 八、项目中常见类型文件组织方式

### 1. types 目录应该放什么

`types` 目录放类型定义：

- 后端返回类型。
- 表单类型。
- 查询参数类型。
- 表格行数据类型。
- 状态字面量类型。

### 2. api 目录应该放什么

`api` 目录放接口请求函数：

- 不写页面逻辑。
- 不写弹窗逻辑。
- 只负责请求地址、请求方法、参数和返回类型。

### 3. views 页面中应该写什么

`views` 里写页面逻辑：

- 查询条件。
- 表格数据。
- 弹窗显示隐藏。
- 调用 API。
- 表单提交。
- 分页切换。

### 4. 为什么不要把所有类型都写在一个 Vue 文件里

不推荐：

```vue
<script setup lang="ts">
interface Book {}
interface BookForm {}
interface BookQuery {}
</script>
```

如果只有一个小页面可以这样写，但项目变大后会有问题：

- 其他页面无法复用。
- API 文件也需要类型。
- 类型散落各处，不好维护。
- 字段变化时要到处改。

### 5. 推荐目录结构

```text
src
 ├─ api
 │   └─ book.ts
 ├─ types
 │   ├─ common.ts
 │   └─ book.ts
 ├─ views
 │   └─ book
 │       └─ index.vue
 ├─ utils
 │   └─ request.ts
 ├─ components
 ├─ router
 └─ stores
```

---

## 九、TypeScript 常见报错解释

### 1. Type 'xxx' is not assignable to type 'xxx'

原因：赋值类型不匹配。

错误代码：

```ts
let age: number = 18
age = '18'
```

正确代码：

```ts
let age: number = 18
age = 20
```

项目中如何避免：

- 表单数字字段用数字。
- 后端返回字符串时不要直接当数字用。
- 必要时转换：`Number(value)`。

### 2. Object is possibly 'null'

原因：对象可能是 null，不能直接使用。

错误代码：

```ts
const currentBook = ref<Book | null>(null)
console.log(currentBook.value.name)
```

正确代码：

```ts
if (currentBook.value) {
  console.log(currentBook.value.name)
}
```

或者：

```ts
console.log(currentBook.value?.name)
```

项目中如何避免：

- `ref<T | null>(null)` 使用前先判断。
- 调用子组件方法时用 `?.`。

### 3. Property 'xxx' does not exist on type 'xxx'

原因：访问了类型里没有定义的字段。

错误代码：

```ts
interface Book {
  id: number
  name: string
}

const book: Book = { id: 1, name: 'Vue3' }
console.log(book.price)
```

正确代码：

```ts
interface Book {
  id: number
  name: string
  price: number
}
```

项目中如何避免：

- 后端新增字段后，同步更新类型。
- 字段名和后端保持一致。

### 4. Parameter implicitly has an 'any' type

原因：函数参数没有写类型，TS 不知道它是什么。

错误代码：

```ts
const handleDelete = (row) => {
  console.log(row.id)
}
```

正确代码：

```ts
const handleDelete = (row: Book): void => {
  console.log(row.id)
}
```

项目中如何避免：

- 表格行参数写类型。
- 事件参数写类型。
- 工具函数参数写类型。

### 5. Type 'undefined' is not assignable

原因：某个值可能是 undefined，但目标类型不允许。

错误代码：

```ts
interface Query {
  keyword: string
}

const keyword: string | undefined = undefined
const query: Query = {
  keyword
}
```

正确代码：

```ts
const query: Query = {
  keyword: keyword || ''
}
```

项目中如何避免：

- 可选字段用 `?`。
- 给默认值。

### 6. Argument of type 'xxx' is not assignable

原因：调用函数时传参类型不对。

错误代码：

```ts
function deleteBook(id: number): void {}

deleteBook('1')
```

正确代码：

```ts
deleteBook(1)
```

或者：

```ts
deleteBook(Number('1'))
```

### 7. axios 返回值类型不明确

原因：没有给请求函数定义返回类型。

不推荐：

```ts
export const getBookPage = (params: any) => {
  return request.get('/books/page', { params })
}
```

推荐：

```ts
export const getBookPage = (params: BookQuery): Promise<Result<PageResult<Book>>> => {
  return request.get('/books/page', { params })
}
```

项目中如何避免：

- 每个 API 函数都写参数类型和返回类型。

### 8. ref(null) 后面调用方法报错

错误代码：

```ts
const formRef = ref(null)
formRef.value.validate()
```

正确代码：

```ts
import type { FormInstance } from 'element-plus'

const formRef = ref<FormInstance>()

const submit = async (): Promise<void> => {
  if (!formRef.value) return
  await formRef.value.validate()
}
```

项目中如何避免：

- Element Plus 组件 ref 要写组件实例类型。
- 使用前先判断是否存在。

---

## 十、从 JavaScript 迁移到 TypeScript 的建议

### 1. 初学者应该先掌握哪些内容

优先掌握：

- 基础类型。
- 数组和对象类型。
- 联合类型。
- `interface`。
- `type`。
- 函数参数和返回值类型。
- `ref`、`reactive`。
- `defineProps`、`defineEmits`。
- Axios 接口返回类型。
- Element Plus 表单和表格类型。

### 2. 哪些高级语法可以暂时不用学

可以暂时放一放：

- 复杂泛型。
- 条件类型。
- 映射类型。
- 装饰器。
- 类型体操。
- 高级工具类型组合。
- `infer`。

这些不是你开始写 Vue 后台项目的门槛。

### 3. 项目中什么时候可以先用 any

可以临时用：

- 第三方库类型不清楚。
- 后端返回结构经常变。
- 先赶进度跑通功能。
- 非核心页面的临时数据。

示例：

```ts
const rawData = ref<any>(null)
```

### 4. 为什么不建议到处使用 any

因为 `any` 会让 TS 失去意义。

```ts
const book: any = {}
book.name.xxx.yyy()
```

这段代码 TS 不会报错，但运行时很可能崩。

建议：可以短期用，但后面要逐步替换成明确类型。

### 5. 如何逐步把 JS 项目改成 TS 项目

推荐顺序：

1. 新建 `types` 目录，先写业务对象类型。
2. 把 `request.js` 改成 `request.ts`。
3. 把 `api/*.js` 改成 `api/*.ts`。
4. 把 Pinia store 改成 TS。
5. 新页面直接用 `<script setup lang="ts">`。
6. 老页面不用急着全改，边维护边迁移。

### 6. Vue 项目最值得优先加类型的地方

最值得加：

- 接口请求参数。
- 接口返回值。
- 表单对象。
- 表格行数据。
- props。
- emits。
- Element Plus formRef。
- 当前选中行。

---

## 十一、完整案例：图书管理页面

下面是一个较完整的 Vue3 + TypeScript + Element Plus + Axios 图书管理页面案例。

### 1. 类型定义文件：src/types/common.ts

```ts
export interface Result<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}
```

作用：定义项目通用返回结构，其他模块都可以复用。

### 2. 类型定义文件：src/types/book.ts

```ts
export type BookStatus = 'normal' | 'disabled'

export interface Book {
  id: number
  name: string
  author: string
  price: number
  stock: number
  status: BookStatus
  createTime: string
}

export interface BookQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: BookStatus | ''
}

export interface BookForm {
  id?: number
  name: string
  author: string
  price: number
  stock: number
  status: BookStatus
}
```

作用：定义图书列表、查询条件、表单对象。

### 3. 请求工具：src/utils/request.ts

```ts
import axios from 'axios'
import type { AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    if (res.code !== 200) {
      ElMessage.error(res.msg || '操作失败')
      return Promise.reject(new Error(res.msg || '操作失败'))
    }

    return res
  },
  (error) => {
    ElMessage.error(error.message || '请求失败')
    return Promise.reject(error)
  }
)

export default request
```

作用：统一处理 token、错误提示和后端返回格式。

### 4. API 请求文件：src/api/book.ts

```ts
import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type { Book, BookForm, BookQuery } from '@/types/book'

export const getBookPage = (params: BookQuery): Promise<Result<PageResult<Book>>> => {
  return request.get('/books/page', { params })
}

export const addBook = (data: BookForm): Promise<Result<null>> => {
  return request.post('/books', data)
}

export const updateBook = (data: BookForm): Promise<Result<null>> => {
  return request.put(`/books/${data.id}`, data)
}

export const deleteBook = (id: number): Promise<Result<null>> => {
  return request.delete(`/books/${id}`)
}

export const batchDeleteBook = (ids: number[]): Promise<Result<null>> => {
  return request.delete('/books/batch', { data: ids })
}
```

作用：封装图书模块所有请求，页面只调用函数，不关心请求细节。

### 5. Vue 页面文件：src/views/book/index.vue

```vue
<template>
  <div class="book-page">
    <el-form :model="queryParams" inline class="search-form">
      <el-form-item label="关键字">
        <el-input
          v-model="queryParams.keyword"
          placeholder="请输入图书名称或作者"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-form-item>

      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option label="正常" value="normal" />
          <el-option label="停用" value="disabled" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openAddDialog">新增图书</el-button>
      <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
        批量删除
      </el-button>
    </div>

    <el-table
      :data="tableData"
      v-loading="loading"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="图书名称" min-width="160" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'normal' ? 'success' : 'info'">
            {{ row.status === 'normal' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增图书' : '编辑图书'"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="图书名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入图书名称" />
        </el-form-item>

        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者" />
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>

        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="normal">正常</el-radio>
            <el-radio value="disabled">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addBook, batchDeleteBook, deleteBook, getBookPage, updateBook } from '@/api/book'
import type { Book, BookForm, BookQuery } from '@/types/book'

type DialogMode = 'add' | 'edit'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('add')
const total = ref(0)

const tableData = ref<Book[]>([])
const selectedRows = ref<Book[]>([])
const formRef = ref<FormInstance>()

const queryParams = reactive<BookQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
})

const form = reactive<BookForm>({
  name: '',
  author: '',
  price: 0,
  stock: 0,
  status: 'normal'
})

const rules = reactive<FormRules<BookForm>>({
  name: [{ required: true, message: '请输入图书名称', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

const loadData = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getBookPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = (): void => {
  queryParams.pageNum = 1
  loadData()
}

const resetQuery = (): void => {
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.keyword = ''
  queryParams.status = ''
  loadData()
}

const handleSizeChange = (): void => {
  queryParams.pageNum = 1
  loadData()
}

const handleSelectionChange = (rows: Book[]): void => {
  selectedRows.value = rows
}

const openAddDialog = (): void => {
  dialogMode.value = 'add'
  dialogVisible.value = true
}

const openEditDialog = (row: Book): void => {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    author: row.author,
    price: row.price,
    stock: row.stock,
    status: row.status
  })
  dialogVisible.value = true
}

const resetForm = (): void => {
  Object.assign(form, {
    id: undefined,
    name: '',
    author: '',
    price: 0,
    stock: 0,
    status: 'normal'
  })
  formRef.value?.clearValidate()
}

const submitForm = async (): Promise<void> => {
  if (!formRef.value) return

  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      await addBook(form)
      ElMessage.success('新增成功')
    } else {
      await updateBook(form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: Book): Promise<void> => {
  await ElMessageBox.confirm(`确定删除《${row.name}》吗？`, '提示', {
    type: 'warning'
  })
  await deleteBook(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleBatchDelete = async (): Promise<void> => {
  const ids = selectedRows.value.map((item) => item.id)
  await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 本图书吗？`, '提示', {
    type: 'warning'
  })
  await batchDeleteBook(ids)
  ElMessage.success('批量删除成功')
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.book-page {
  padding: 16px;
}

.search-form {
  margin-bottom: 12px;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
```

作用：这是一个完整的列表页案例，包含查询、新增、编辑、删除、分页、表单校验、表格多选。

---

## Vue + TS 项目开发常用速查表

### 基础类型

```ts
const name: string = '张三'
const age: number = 18
const loading: boolean = false
const list: Book[] = []
const current: Book | null = null
```

### 常用业务类型

```ts
interface Result<T> {
  code: number
  msg: string
  data: T
}

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}
```

### ref

```ts
const keyword = ref<string>('')
const total = ref<number>(0)
const visible = ref<boolean>(false)
const list = ref<Book[]>([])
const current = ref<Book | null>(null)
```

### reactive

```ts
const form = reactive<BookForm>({
  name: '',
  price: 0
})
```

### props

```ts
interface Props {
  title: string
  visible?: boolean
}

const props = defineProps<Props>()
```

### emits

```ts
const emit = defineEmits<{
  success: []
  select: [row: Book]
}>()
```

### Element Plus formRef

```ts
const formRef = ref<FormInstance>()

if (!formRef.value) return
await formRef.value.validate()
```

### API 函数

```ts
export const getBookPage = (params: BookQuery): Promise<Result<PageResult<Book>>> => {
  return request.get('/books/page', { params })
}
```

### 表格多选

```ts
const selectedRows = ref<Book[]>([])

const handleSelectionChange = (rows: Book[]): void => {
  selectedRows.value = rows
}
```

### 空值安全

```ts
currentBook.value?.name

if (currentBook.value) {
  console.log(currentBook.value.name)
}
```

---

## 参考资料

- [TypeScript Handbook: Everyday Types](https://www.typescriptlang.org/docs/handbook/2/everyday-types.html)
- [Vue 官方文档：TypeScript with Composition API](https://vuejs.org/guide/typescript/composition-api)
- [Element Plus 官方文档：Form](https://element-plus.org/en-US/component/form)
- [Element Plus 官方文档：Table](https://element-plus.org/en-US/component/table)
- [Axios 官方文档：Getting Started](https://axios-http.com/docs/intro)
- [Axios 官方文档：Response Schema](https://axios-http.com/docs/res_schema)
