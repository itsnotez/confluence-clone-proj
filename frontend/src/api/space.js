import api from './axios'

export default {
  getSpaces: () => api.get('/spaces'),
  getSpace: (spaceKey) => api.get(`/spaces/${spaceKey}`),
  createSpace: (data) => api.post('/spaces', data),
  updateSpace: (spaceKey, data) => api.put(`/spaces/${spaceKey}`, data),
  deleteSpace: (spaceKey) => api.delete(`/spaces/${spaceKey}`),
  toggleFavorite: (spaceKey) => api.post(`/spaces/${spaceKey}/favorite`),
  getContentTree: (spaceKey) => api.get(`/spaces/${spaceKey}/contents`)
}
