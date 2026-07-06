import api from './axios'

export const labelApi = {
  listBySpace: (spaceId) => api.get('/labels', { params: { spaceId } }),
  create: (data) => api.post('/labels', data),
  getByContent: (contentId) => api.get(`/contents/${contentId}/labels`),
  add: (contentId, labelId) => api.post(`/contents/${contentId}/labels`, { labelId }),
  remove: (contentId, labelId) => api.delete(`/contents/${contentId}/labels/${labelId}`),
}
