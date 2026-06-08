import { useUserStore } from '@/stores/user'
import { computed } from 'vue'

export function usePermission() {
  const userStore = useUserStore()

  const currentRole = computed(() => userStore.role)

  function hasRole(roleCode) {
    return userStore.role === roleCode
  }

  function hasAnyRole(...roleCodes) {
    return roleCodes.includes(userStore.role)
  }

  return { currentRole, hasRole, hasAnyRole }
}
