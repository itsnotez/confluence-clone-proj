import api from './axios'

export const mailAccountApi = {
  getAccounts: (spaceKey) => api.get(`/spaces/${spaceKey}/mail-accounts`),
  createAccount: (spaceKey, data) => api.post(`/spaces/${spaceKey}/mail-accounts`, data),
  deleteAccount: (spaceKey, id) => api.delete(`/spaces/${spaceKey}/mail-accounts/${id}`),
}

export const mailMessageApi = {
  getMessages: (spaceKey, accountId) => api.get(`/spaces/${spaceKey}/mail-accounts/${accountId}/messages`),
  convertToPage: (spaceKey, accountId, msgId) => api.post(`/spaces/${spaceKey}/mail-accounts/${accountId}/messages/${msgId}/convert`),
}
