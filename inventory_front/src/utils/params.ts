export function cleanParams<T extends Record<string, unknown>>(params: T) {
  const result: Record<string, unknown> = {}

  Object.entries(params).forEach(([key, value]) => {
    if (value !== '' && value !== undefined && value !== null) {
      result[key] = value
    }
  })

  return result
}
