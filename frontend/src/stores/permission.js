import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 角色与菜单权限映射
 * 每个角色对应可访问的路由前缀
 */
const roleRouteMap = {
  ADMIN: ['/admin'],
  ACADEMIC: ['/academic'],
  DIRECTOR: ['/director', '/academic/dashboard', '/academic/reports'],
  TEACHER: ['/teacher']
}

export const usePermissionStore = defineStore('permission', () => {
  const accessibleRoutes = ref([])

  /**
   * 根据角色生成可访问路由
   * @param {string} roleCode - 角色编码
   * @returns {string[]} 可访问的路由前缀列表
   */
  function generateRoutes(roleCode) {
    const prefixes = roleRouteMap[roleCode] || []
    accessibleRoutes.value = prefixes
    return prefixes
  }

  /**
   * 判断当前角色是否可访问指定路径
   * @param {string} path - 路由路径
   * @param {string} roleCode - 角色编码
   * @returns {boolean}
   */
  function hasPermission(path, roleCode) {
    if (!roleCode) return false
    const prefixes = roleRouteMap[roleCode] || []
    return prefixes.some(prefix => path.startsWith(prefix))
  }

  return { accessibleRoutes, generateRoutes, hasPermission }
})
