import api from './axios'

export default {
  getContentTree: (spaceKey) => api.get(`/spaces/${spaceKey}/contents`),
  createContent: (spaceKey, data) => api.post(`/spaces/${spaceKey}/contents`, data),
  getContent: (id) => api.get(`/contents/${id}`),
  updateContent: (id, data) => api.put(`/contents/${id}`, data),
  publishContent: (id, data) => api.post(`/contents/${id}/publish`, data),
  deleteContent: (id) => api.delete(`/contents/${id}`),
  getVersions: (id) => api.get(`/contents/${id}/versions`)
}
