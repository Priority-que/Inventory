export function formatDate(value?: string | null) {
  if (!value) {
    return '-'
  }

  return value.split('T')[0] || value
}

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

export function formatArray(values?: Array<string | number> | null, separator = '、') {
  if (!values?.length) {
    return '-'
  }

  return values.join(separator)
}
