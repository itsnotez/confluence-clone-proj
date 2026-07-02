import api from './axios'

export default {
  getSpacePermissions: (spaceKey) => api.get(`/spaces/${spaceKey}/permissions`),
  grantPermission: (spaceKey, data) => api.post(`/spaces/${spaceKey}/permissions`, data),
  revokePermission: (spaceKey, subjectType, subjectId) =>
    api.delete(`/spaces/${spaceKey}/permissions`, { params: { subjectType, subjectId } })
}
