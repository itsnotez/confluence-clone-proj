import api from './axios'

export const commentApi = {
  list:   (contentId) => api.get(`/contents/${contentId}/comments`),
  create: (contentId, data) => api.post(`/contents/${contentId}/comments`, data),
  remove: (id) => api.delete(`/comments/${id}`),
}
