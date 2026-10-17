import { apiRequest } from './api.js'

/**
 * Fetches the public community complaints feed.
 *
 * Backend: GET /api/v1/community/complaints?status={status}&sort={sort}
 *
 * @param {Object} [params]
 * @param {string} [params.status] - Optional filter: SUBMITTED, IN_PROGRESS, RESOLVED
 * @param {string} [params.sort='RECENT'] - RECENT or MOST_SUPPORTED
 * @returns {Promise<Array>} Array of CommunityComplaintResponse objects
 */
export function getCommunityFeed({ status, sort = 'RECENT' } = {}) {
  const query = new URLSearchParams()
  if (status) query.append('status', status)
  if (sort) query.append('sort', sort)

  const queryString = query.toString()
  const path = queryString ? `/community/complaints?${queryString}` : '/community/complaints'

  // Pass auth: true so if token exists, supportedByCurrentUser reflects user's state;
  // if not authenticated, apiRequest safely omits Authorization and supportedByCurrentUser is false.
  return apiRequest(path, {
    method: 'GET',
    auth: true,
  })
}

/**
 * Fetches community complaints nearby the user's location.
 *
 * Backend: GET /api/v1/community/complaints/nearby?latitude={lat}&longitude={lon}&radiusKm={radius}
 *
 * @param {Object} params
 * @param {number} [params.latitude]
 * @param {number} [params.longitude]
 * @param {number} [params.radiusKm=5.0]
 * @returns {Promise<Array>} Array of CommunityComplaintResponse objects
 */
export function getNearbyCommunityComplaints({ latitude, longitude, radiusKm = 5.0 } = {}) {
  const query = new URLSearchParams()
  if (latitude != null) query.append('latitude', latitude)
  if (longitude != null) query.append('longitude', longitude)
  if (radiusKm != null) query.append('radiusKm', radiusKm)

  const queryString = query.toString()
  const path = queryString ? `/community/complaints/nearby?${queryString}` : '/community/complaints/nearby'

  return apiRequest(path, {
    method: 'GET',
    auth: true,
  })
}

/**
 * Fetches the community details of a single complaint.
 *
 * Backend: GET /api/v1/community/complaints/{id}
 *
 * @param {string} complaintId
 * @returns {Promise<Object>} CommunityComplaintResponse
 */
export function getCommunityComplaint(complaintId) {
  return apiRequest(`/community/complaints/${complaintId}`, {
    method: 'GET',
    auth: true,
  })
}

/**
 * Supports a complaint with "I'm affected too".
 *
 * Backend: POST /api/v1/community/complaints/{id}/support
 *
 * @param {string} complaintId
 * @returns {Promise<Object>} ComplaintCommunityResponse
 */
export function supportComplaint(complaintId) {
  return apiRequest(`/community/complaints/${complaintId}/support`, {
    method: 'POST',
    auth: true,
  })
}

/**
 * Removes "I'm affected too" support from a complaint.
 *
 * Backend: DELETE /api/v1/community/complaints/{id}/support
 *
 * @param {string} complaintId
 * @returns {Promise<Object>} ComplaintCommunityResponse
 */
export function removeSupport(complaintId) {
  return apiRequest(`/community/complaints/${complaintId}/support`, {
    method: 'DELETE',
    auth: true,
  })
}

/**
 * Fetches the M6 corroboration intelligence for a complaint.
 *
 * Backend: GET /api/v1/community/complaints/{id}/corroboration
 *
 * @param {string} complaintId
 * @returns {Promise<Object>} CorroborationIntelligence
 */
export function getCorroboration(complaintId) {
  return apiRequest(`/community/complaints/${complaintId}/corroboration`, {
    method: 'GET',
    auth: false,
  })
}

/**
 * Fetches paginated comments for a complaint.
 *
 * Backend: GET /api/v1/community/complaints/{id}/comments?page={page}&size={size}
 *
 * @param {string} complaintId
 * @param {Object} [pagination]
 * @param {number} [pagination.page=0]
 * @param {number} [pagination.size=20]
 * @returns {Promise<Object>} Spring Page object containing content: CommunityCommentResponse[]
 */
export function getComments(complaintId, { page = 0, size = 20 } = {}) {
  const query = new URLSearchParams()
  query.append('page', Math.max(0, page))
  query.append('size', Math.min(Math.max(1, size), 50))

  return apiRequest(`/community/complaints/${complaintId}/comments?${query.toString()}`, {
    method: 'GET',
    auth: true,
  })
}

/**
 * Adds a new comment to a complaint.
 *
 * Backend: POST /api/v1/community/complaints/{id}/comments
 *
 * @param {string} complaintId
 * @param {Object} data
 * @param {string} data.content
 * @returns {Promise<Object>} CommunityCommentResponse
 */
export function createComment(complaintId, { content }) {
  return apiRequest(`/community/complaints/${complaintId}/comments`, {
    method: 'POST',
    body: { content },
    auth: true,
  })
}

/**
 * Updates an existing comment owned by the current user.
 *
 * Backend: PUT /api/v1/community/comments/{commentId}
 *
 * @param {string} commentId
 * @param {Object} data
 * @param {string} data.content
 * @returns {Promise<Object>} CommunityCommentResponse
 */
export function updateComment(commentId, { content }) {
  return apiRequest(`/community/comments/${commentId}`, {
    method: 'PUT',
    body: { content },
    auth: true,
  })
}

/**
 * Deletes an existing comment owned by the current user.
 *
 * Backend: DELETE /api/v1/community/comments/{commentId}
 *
 * @param {string} commentId
 * @returns {Promise<null>}
 */
export function deleteComment(commentId) {
  return apiRequest(`/community/comments/${commentId}`, {
    method: 'DELETE',
    auth: true,
  })
}


