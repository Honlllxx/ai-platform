import { ref, reactive, computed, type Ref } from 'vue'

interface PaginationState {
  page: number
  pageSize: number
  total: number
}

interface UseTableOptions<T> {
  fetchApi: (params: any) => Promise<any>
  defaultQuery?: Record<string, unknown>
  defaultPageSize?: number
  onSuccess?: (data: T[]) => void
  onError?: (error: any) => void
}

export function useTable<T>(options: UseTableOptions<T>) {
  const tableRef = ref()
  const loading = ref(false)
  const data = ref<T[]>([]) as Ref<T[]>
  const pagination = reactive<PaginationState>({
    page: 1,
    pageSize: options.defaultPageSize || 10,
    total: 0
  })
  const query = reactive<Record<string, unknown>>({ ...options.defaultQuery })

  const hasData = computed(() => data.value.length > 0)

  async function fetchData() {
    loading.value = true
    try {
      const params = {
        ...query,
        page: pagination.page,
        pageSize: pagination.pageSize
      }
      const res = await options.fetchApi(params)
      const result = res?.data || res
      data.value = result.records || result.list || result.data || []
      pagination.total = result.total || 0
      options.onSuccess?.(data.value)
    } catch (error) {
      data.value = []
      pagination.total = 0
      options.onError?.(error)
    } finally {
      loading.value = false
    }
  }

  function handleSearch(newQuery?: Record<string, unknown>) {
    if (newQuery) {
      Object.assign(query, newQuery)
    }
    pagination.page = 1
    fetchData()
  }

  function handleReset(resetQuery: Record<string, unknown> = {}) {
    Object.keys(query).forEach(key => {
      if (resetQuery.hasOwnProperty(key)) {
        query[key] = resetQuery[key]
      } else {
        delete query[key]
      }
    })
    pagination.page = 1
    fetchData()
  }

  function handlePageChange(page: number) {
    pagination.page = page
    fetchData()
  }

  function handleSizeChange(size: number) {
    pagination.pageSize = size
    pagination.page = 1
    fetchData()
  }

  function refresh() {
    fetchData()
  }

  return {
    tableRef,
    loading,
    data,
    pagination,
    query,
    hasData,
    fetchData,
    handleSearch,
    handleReset,
    handlePageChange,
    handleSizeChange,
    refresh
  }
}
