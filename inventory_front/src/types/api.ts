export interface ApiResult<T = unknown>{
    code: number
    msg: string
    data: T
}
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}