export function formatDateTime(value?: string | null) {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ')
}

export function formatEmpty(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  return value
}
