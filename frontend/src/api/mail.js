import api from './axios'

export const mailAccountApi = {
  getAccounts: (spaceKey) => api.get(`/spaces/${spaceKey}/mail-accounts`),
  createAccount: (spaceKey, data) => api.post(`/spaces/${spaceKey}/mail-accounts`, data),
  deleteAccount: (spaceKey, id) => api.delete(`/spaces/${spaceKey}/mail-accounts/${id}`),
  syncAccount: (spaceKey, id) => api.post(`/spaces/${spaceKey}/mail-accounts/${id}/sync`),
}

export const mailMessageApi = {
  getMessages: (spaceKey, accountId) => api.get(`/spaces/${spaceKey}/mail-accounts/${accountId}/messages`),
  convertToPage: (spaceKey, accountId, msgId) => api.post(`/spaces/${spaceKey}/mail-accounts/${accountId}/messages/${msgId}/convert`),
  getAttachments: (spaceKey, accountId, msgId) =>
    api.get(`/spaces/${spaceKey}/mail-accounts/${accountId}/messages/${msgId}/attachments`),
  downloadAttachment: (spaceKey, accountId, msgId, attachId) =>
    api.get(`/spaces/${spaceKey}/mail-accounts/${accountId}/messages/${msgId}/attachments/${attachId}/download`,
      { responseType: 'blob' }),
}
