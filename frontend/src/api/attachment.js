import api from './axios'

export const attachmentApi = {
  list: (contentId) => api.get(`/contents/${contentId}/attachments`),
  upload: (contentId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/contents/${contentId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  download: (id) => `/api/attachments/${id}/download`,
}
