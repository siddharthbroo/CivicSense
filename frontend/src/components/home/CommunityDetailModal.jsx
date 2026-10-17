import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import {
  getCommunityComplaint,
  getCorroboration,
  getComments,
  createComment,
  updateComment,
  deleteComment,
} from '../../api/communityApi.js'
import {
  LocationPinIcon,
  UsersGroupIcon,
  ChatBubbleIcon,
  ClockIcon,
  CloseIcon,
  ThumbsUpIcon,
  PencilIcon,
  TrashCanIcon,
} from './HomeIcons.jsx'
import Spinner from '../common/Spinner.jsx'

export default function CommunityDetailModal({
  complaintId,
  onClose,
  onSupportToggle,
  isSupporting = false,
  onCommentCountChange,
}) {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [complaint, setComplaint] = useState(null)
  const [corroboration, setCorroboration] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  // Comments state
  const [comments, setComments] = useState([])
  const [isCommentsLoading, setIsCommentsLoading] = useState(true)
  const [commentsError, setCommentsError] = useState('')
  const [commentsPage, setCommentsPage] = useState(0)
  const [hasMoreComments, setHasMoreComments] = useState(false)
  const [isLoadingMore, setIsLoadingMore] = useState(false)

  // New comment composer state
  const [newCommentText, setNewCommentText] = useState('')
  const [isSubmittingComment, setIsSubmittingComment] = useState(false)
  const [submitError, setSubmitError] = useState('')

  // Inline editing state
  const [editingCommentId, setEditingCommentId] = useState(null)
  const [editingText, setEditingText] = useState('')
  const [isUpdatingComment, setIsUpdatingComment] = useState(false)
  const [updateError, setUpdateError] = useState('')

  // Deletion confirmation state
  const [deletingCommentId, setDeletingCommentId] = useState(null)
  const [isDeletingComment, setIsDeletingComment] = useState(false)
  const [deleteError, setDeleteError] = useState('')

  // Load complaint details, corroboration, and initial comments concurrently
  useEffect(() => {
    let ignore = false

    async function loadDetails() {
      if (!complaintId) return
      setIsLoading(true)
      setIsCommentsLoading(true)
      setError('')
      setCommentsError('')

      try {
        const [complaintRes, corroborationRes, commentsRes] = await Promise.allSettled([
          getCommunityComplaint(complaintId),
          getCorroboration(complaintId),
          getComments(complaintId, { page: 0, size: 20 }),
        ])

        if (!ignore) {
          if (complaintRes.status === 'fulfilled') {
            setComplaint(complaintRes.value)
          } else {
            throw new Error(complaintRes.reason?.message || 'Failed to load complaint details')
          }

          if (corroborationRes.status === 'fulfilled') {
            setCorroboration(corroborationRes.value)
          }

          if (commentsRes.status === 'fulfilled') {
            const res = commentsRes.value
            if (res && Array.isArray(res.content)) {
              setComments(res.content)
              setCommentsPage(res.number || 0)
              setHasMoreComments(res.number < (res.totalPages || 0) - 1)
            } else {
              setComments([])
              setHasMoreComments(false)
            }
          } else {
            setCommentsError(commentsRes.reason?.message || 'Unable to load comments.')
          }
        }
      } catch (err) {
        if (!ignore) {
          setError(err.message || 'Unable to load report details.')
        }
      } finally {
        if (!ignore) {
          setIsLoading(false)
          setIsCommentsLoading(false)
        }
      }
    }

    loadDetails()
    return () => {
      ignore = true
    }
  }, [complaintId])

  // Load more comments pagination
  async function handleLoadMore() {
    if (isLoadingMore || !hasMoreComments) return
    setIsLoadingMore(true)
    setCommentsError('')
    try {
      const nextPage = commentsPage + 1
      const res = await getComments(complaintId, { page: nextPage, size: 20 })
      if (res && Array.isArray(res.content)) {
        setComments((prev) => [...prev, ...res.content])
        setCommentsPage(res.number || nextPage)
        setHasMoreComments(res.number < (res.totalPages || 0) - 1)
      }
    } catch (err) {
      console.error('Failed to load more comments:', err)
      setCommentsError(err.message || 'Unable to load older comments.')
    } finally {
      setIsLoadingMore(false)
    }
  }

  // Handle ESC key to close
  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  // Create comment
  async function handleCreateComment(e) {
    e.preventDefault()
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: '/dashboard' } } })
      return
    }

    const trimmed = newCommentText.trim()
    if (!trimmed) return
    if (trimmed.length > 1000) {
      setSubmitError('Comment cannot exceed 1000 characters.')
      return
    }

    setIsSubmittingComment(true)
    setSubmitError('')

    try {
      const created = await createComment(complaintId, { content: trimmed })
      setComments((prev) => [created, ...prev])
      setNewCommentText('')

      // Increment internal count and inform parent
      setComplaint((prev) =>
        prev ? { ...prev, commentCount: (prev.commentCount || 0) + 1 } : prev
      )
      onCommentCountChange?.(complaintId, 1)
    } catch (err) {
      console.error('Failed to create comment:', err)
      setSubmitError(err.message || 'Failed to post comment. Please try again.')
    } finally {
      setIsSubmittingComment(false)
    }
  }

  // Start inline editing
  function handleStartEdit(comment) {
    setEditingCommentId(comment.id)
    setEditingText(comment.content)
    setUpdateError('')
  }

  // Cancel inline editing
  function handleCancelEdit() {
    setEditingCommentId(null)
    setEditingText('')
    setUpdateError('')
  }

  // Save inline edit
  async function handleSaveEdit(commentId) {
    const trimmed = editingText.trim()
    if (!trimmed) {
      setUpdateError('Comment cannot be empty.')
      return
    }
    if (trimmed.length > 1000) {
      setUpdateError('Comment cannot exceed 1000 characters.')
      return
    }

    setIsUpdatingComment(true)
    setUpdateError('')

    try {
      const updated = await updateComment(commentId, { content: trimmed })
      setComments((prev) =>
        prev.map((c) => (c.id === commentId ? updated : c))
      )
      setEditingCommentId(null)
      setEditingText('')
    } catch (err) {
      console.error('Failed to update comment:', err)
      setUpdateError(err.message || 'Failed to save changes. You may not own this comment.')
    } finally {
      setIsUpdatingComment(false)
    }
  }

  // Confirm delete comment
  async function handleConfirmDelete(commentId) {
    setIsDeletingComment(true)
    setDeleteError('')

    try {
      await deleteComment(commentId)
      setComments((prev) => prev.filter((c) => c.id !== commentId))
      setDeletingCommentId(null)

      // Decrement internal count and inform parent
      setComplaint((prev) =>
        prev ? { ...prev, commentCount: Math.max(0, (prev.commentCount || 0) - 1) } : prev
      )
      onCommentCountChange?.(complaintId, -1)
    } catch (err) {
      console.error('Failed to delete comment:', err)
      setDeleteError(err.message || 'Failed to delete comment. You may not own this comment.')
    } finally {
      setIsDeletingComment(false)
    }
  }

  function getBadge(c) {
    if (!c) return { label: 'Under Review', className: 'bg-amber-500 text-white' }
    if (c.status === 'RESOLVED') {
      return { label: 'Resolved', className: 'bg-emerald-600 text-white' }
    }
    if (c.priority === 'HIGH' || c.priority === 'CRITICAL') {
      return { label: 'High Priority', className: 'bg-red-600 text-white' }
    }
    if (c.status === 'IN_PROGRESS') {
      return { label: 'In Progress', className: 'bg-amber-600 text-white' }
    }
    return { label: 'Under Review', className: 'bg-amber-500 text-white' }
  }

  function formatDateTime(dateString) {
    if (!dateString) return 'Not available'
    try {
      return new Date(dateString).toLocaleString(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
      })
    } catch {
      return dateString
    }
  }

  function formatRelativeTime(dateString) {
    if (!dateString) return 'recently'
    try {
      const now = new Date()
      const past = new Date(dateString)
      const diffSec = Math.floor((now - past) / 1000)
      if (diffSec < 60) return 'just now'
      const diffMin = Math.floor(diffSec / 60)
      if (diffMin < 60) return `${diffMin}m ago`
      const diffHrs = Math.floor(diffMin / 60)
      if (diffHrs < 24) return `${diffHrs}h ago`
      const diffDays = Math.floor(diffHrs / 24)
      if (diffDays === 1) return '1d ago'
      if (diffDays < 7) return `${diffDays}d ago`
      return past.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
    } catch {
      return 'recently'
    }
  }

  const badge = getBadge(complaint)

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-2.5 sm:p-6 bg-slate-950/70 backdrop-blur-sm animate-in fade-in duration-200"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="relative w-full max-w-2xl max-h-[92dvh] sm:max-h-[90vh] overflow-y-auto bg-white rounded-2xl sm:rounded-3xl shadow-2xl border border-slate-100 flex flex-col animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header Close Button */}
        <button
          type="button"
          onClick={onClose}
          aria-label="Close modal"
          className="absolute top-4 right-4 z-20 p-2 rounded-full bg-white/90 hover:bg-white text-slate-700 hover:text-slate-900 shadow-md transition-all active:scale-95 cursor-pointer"
        >
          <CloseIcon className="w-5 h-5" />
        </button>

        {isLoading ? (
          <div className="p-16 flex flex-col items-center justify-center text-center">
            <Spinner size={32} className="text-emerald-600 mb-3" />
            <p className="text-sm font-semibold text-slate-700">Loading civic report details...</p>
            <p className="text-xs text-slate-400 mt-1">Fetching authoritative corroboration data</p>
          </div>
        ) : error ? (
          <div className="p-12 text-center">
            <p className="text-sm font-semibold text-red-600">{error}</p>
            <button
              type="button"
              onClick={onClose}
              className="mt-4 px-4 py-2 text-xs font-bold rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer"
            >
              Close
            </button>
          </div>
        ) : complaint ? (
          <div className="flex flex-col">
            {/* Top Large Image Banner */}
            <div className="relative h-64 sm:h-72 w-full bg-slate-100 overflow-hidden shrink-0">
              {complaint.imageUrl ? (
                <img
                  src={complaint.imageUrl}
                  alt={complaint.title || 'Civic report photo'}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-emerald-50 to-slate-100 text-slate-400">
                  <LocationPinIcon className="w-12 h-12 text-emerald-400 mb-2" />
                  <span className="text-xs font-semibold text-slate-500">
                    {complaint.category || 'Civic Issue'} (No photo uploaded)
                  </span>
                </div>
              )}

              {/* Status/Priority Badge */}
              <div className="absolute top-4 left-4 z-10 flex items-center gap-2">
                <span
                  className={`inline-flex items-center px-3 py-1 rounded-xl text-xs font-bold shadow-md ${badge.className}`}
                >
                  {badge.label}
                </span>
                {complaint.category && (
                  <span className="inline-flex items-center px-3 py-1 rounded-xl text-xs font-bold bg-black/60 text-white backdrop-blur-md">
                    {complaint.category.replace(/_/g, ' ')}
                  </span>
                )}
              </div>
            </div>

            {/* Modal Body */}
            <div className="p-6 sm:p-8 space-y-6">
              {/* Title and Address */}
              <div>
                <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900 tracking-tight leading-snug">
                  {complaint.title || 'Civic Hazard Report'}
                </h2>
                <div className="flex items-center gap-1.5 mt-2 text-sm text-slate-600 font-medium">
                  <LocationPinIcon className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{complaint.address || 'Address pending verification'}</span>
                </div>
              </div>

              {/* Description */}
              <div className="rounded-2xl bg-slate-50 p-4 border border-slate-100 text-sm text-slate-700 leading-relaxed">
                <p className="font-semibold text-xs text-slate-400 uppercase tracking-wider mb-1">
                  Citizen Report Description
                </p>
                {complaint.description || 'No detailed description provided.'}
              </div>

              {/* Corroboration Intelligence Section */}
              <div className="rounded-2xl bg-emerald-50/60 p-5 border border-emerald-100/80 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
                    <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-900">
                      Community Corroboration Intelligence
                    </h3>
                  </div>
                  {corroboration?.corroborationScore !== undefined && (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-700 text-white font-bold text-xs">
                      Score: {corroboration.corroborationScore}
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1">
                  <div className="bg-white/80 p-3 rounded-xl border border-emerald-100">
                    <p className="text-[11px] font-semibold text-slate-500">Total Reports</p>
                    <p className="text-lg font-extrabold text-slate-900">
                      {corroboration?.totalReports ?? complaint.totalReports ?? 1}
                    </p>
                  </div>
                  <div className="bg-white/80 p-3 rounded-xl border border-emerald-100">
                    <p className="text-[11px] font-semibold text-slate-500">Unique Citizens</p>
                    <p className="text-lg font-extrabold text-slate-900">
                      {corroboration?.uniqueCitizens ?? complaint.uniqueCitizens ?? 1}
                    </p>
                  </div>
                  <div className="bg-white/80 p-3 rounded-xl border border-emerald-100">
                    <p className="text-[11px] font-semibold text-slate-500">Supporters</p>
                    <p className="text-lg font-extrabold text-slate-900">
                      {corroboration?.supportCount ?? complaint.supportCount ?? 0}
                    </p>
                  </div>
                  <div className="bg-white/80 p-3 rounded-xl border border-emerald-100">
                    <p className="text-[11px] font-semibold text-slate-500">Recurrence</p>
                    <p className="text-lg font-extrabold text-slate-900">
                      {(corroboration?.recurring ?? complaint.recurring)
                        ? `Yes (${corroboration?.recurrenceCount ?? complaint.recurrenceCount ?? 0})`
                        : 'No'}
                    </p>
                  </div>
                </div>

                {(corroboration?.firstReportedAt || complaint.firstReportedAt) && (
                  <div className="flex flex-wrap items-center justify-between text-[11px] text-slate-500 pt-1">
                    <span>
                      First reported:{' '}
                      <strong>
                        {formatDateTime(corroboration?.firstReportedAt || complaint.firstReportedAt)}
                      </strong>
                    </span>
                    <span>
                      Last reported:{' '}
                      <strong>
                        {formatDateTime(corroboration?.lastReportedAt || complaint.lastReportedAt)}
                      </strong>
                    </span>
                  </div>
                )}
              </div>

              {/* Engagement & Action Row */}
              <div className="pt-2 flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-6">
                <div className="flex flex-wrap items-center gap-3 sm:gap-4 text-xs text-slate-500">
                  <span className="flex items-center gap-1.5">
                    <UsersGroupIcon className="w-4 h-4 text-emerald-600" />
                    <strong>{complaint.supportCount || 0}</strong> citizens affected
                  </span>
                  <span className="flex items-center gap-1.5">
                    <ChatBubbleIcon className="w-4 h-4 text-slate-400" />
                    <strong>{complaint.commentCount || 0}</strong> comments
                  </span>
                  <span className="flex items-center gap-1.5">
                    <ClockIcon className="w-4 h-4 text-slate-400" />
                    {formatDateTime(complaint.createdAt)}
                  </span>
                </div>

                {/* "I'm affected too" Support Action */}
                <button
                  type="button"
                  onClick={async () => {
                    await onSupportToggle(complaint)
                    // Sync local state
                    setComplaint((prev) =>
                      prev
                        ? {
                            ...prev,
                            supportedByCurrentUser: !prev.supportedByCurrentUser,
                            supportCount: prev.supportedByCurrentUser
                              ? Math.max(0, prev.supportCount - 1)
                              : prev.supportCount + 1,
                          }
                        : prev
                    )
                  }}
                  disabled={isSupporting}
                  className={`inline-flex items-center justify-center gap-2 w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-sm transition-all active:scale-95 cursor-pointer shadow-sm touch-target ${
                    complaint.supportedByCurrentUser
                      ? 'bg-emerald-700 hover:bg-emerald-800 text-white'
                      : 'bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200'
                  }`}
                >
                  {complaint.supportedByCurrentUser ? (
                    <>
                      <span className="w-4 h-4 rounded-full bg-white text-emerald-700 flex items-center justify-center text-xs font-bold">
                        ✓
                      </span>
                      <span>Supported</span>
                    </>
                  ) : (
                    <>
                      <ThumbsUpIcon className="w-4 h-4 text-emerald-700" />
                      <span>I'm affected too</span>
                    </>
                  )}
                </button>
              </div>

              {/* ============================================================== */}
              {/* COMMUNITY DISCUSSION & COMMENTS SECTION */}
              {/* ============================================================== */}
              <div className="space-y-6 pt-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <ChatBubbleIcon className="w-5 h-5 text-emerald-600" />
                    <h3 className="text-base font-extrabold text-slate-900 tracking-tight">
                      Citizen Discussion
                    </h3>
                  </div>
                  <span className="text-xs font-semibold text-slate-500 bg-slate-100 px-2.5 py-1 rounded-full">
                    {complaint.commentCount || 0} {complaint.commentCount === 1 ? 'comment' : 'comments'}
                  </span>
                </div>

                {/* Add Comment Box */}
                {isAuthenticated ? (
                  <form onSubmit={handleCreateComment} className="space-y-3">
                    <div className="relative">
                      <textarea
                        rows={3}
                        maxLength={1000}
                        value={newCommentText}
                        onChange={(e) => {
                          setNewCommentText(e.target.value)
                          if (submitError) setSubmitError('')
                        }}
                        placeholder="Share updates, additional context, or report progress on this issue..."
                        className="w-full px-4 py-3 text-sm rounded-2xl border border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 focus:outline-none transition-all placeholder:text-slate-400 resize-y"
                      />
                      <div className="flex items-center justify-between pt-1 px-1 text-xs">
                        <span
                          className={`font-medium ${
                            newCommentText.length > 950 ? 'text-amber-600 font-bold' : 'text-slate-400'
                          }`}
                        >
                          {newCommentText.length} / 1000
                        </span>
                        {submitError && (
                          <span className="text-red-600 font-semibold">{submitError}</span>
                        )}
                      </div>
                    </div>

                    <div className="flex justify-end">
                      <button
                        type="submit"
                        disabled={isSubmittingComment || !newCommentText.trim() || newCommentText.length > 1000}
                        className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl font-bold text-xs bg-emerald-700 hover:bg-emerald-800 disabled:bg-slate-200 disabled:text-slate-400 disabled:cursor-not-allowed text-white shadow-sm transition-all active:scale-95 cursor-pointer"
                      >
                        {isSubmittingComment ? (
                          <>
                            <Spinner size={14} className="text-white" />
                            <span>Posting...</span>
                          </>
                        ) : (
                          'Post Comment'
                        )}
                      </button>
                    </div>
                  </form>
                ) : (
                  <div className="p-4 rounded-2xl bg-emerald-50/50 border border-emerald-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="text-xs text-slate-600">
                      <p className="font-bold text-slate-800">Have an update or details to share?</p>
                      <p>Sign in to contribute to the discussion and verify progress.</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => navigate('/login', { state: { from: { pathname: '/dashboard' } } })}
                      className="px-4 py-2 rounded-xl text-xs font-bold bg-emerald-700 hover:bg-emerald-800 text-white shadow-xs transition-all active:scale-95 shrink-0 cursor-pointer text-center"
                    >
                      Sign In to Comment
                    </button>
                  </div>
                )}

                {/* Comments List */}
                <div className="space-y-4">
                  {commentsError && (
                    <div className="p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200">
                      {commentsError}
                    </div>
                  )}

                  {deleteError && (
                    <div className="p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200">
                      {deleteError}
                    </div>
                  )}

                  {isCommentsLoading ? (
                    <div className="py-8 flex flex-col items-center justify-center text-slate-400">
                      <Spinner size={24} className="text-emerald-600 mb-2" />
                      <span className="text-xs font-medium">Loading citizen comments...</span>
                    </div>
                  ) : comments.length === 0 ? (
                    <div className="py-8 text-center bg-slate-50/70 rounded-2xl border border-dashed border-slate-200 p-6">
                      <ChatBubbleIcon className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                      <p className="text-sm font-bold text-slate-700">No comments yet</p>
                      <p className="text-xs text-slate-400 mt-0.5">
                        Be the first to share an update or provide more details on this report.
                      </p>
                    </div>
                  ) : (
                    comments.map((comment) => {
                      const isEditing = editingCommentId === comment.id
                      const isDeleting = deletingCommentId === comment.id
                      const isAuthor = comment.editableByCurrentUser || comment.deletableByCurrentUser
                      const wasEdited =
                        comment.updatedAt &&
                        comment.createdAt &&
                        new Date(comment.updatedAt).getTime() - new Date(comment.createdAt).getTime() > 2000

                      return (
                        <div
                          key={comment.id}
                          className="p-4 rounded-2xl bg-white border border-slate-150 shadow-2xs space-y-2.5 transition-all hover:border-slate-250"
                        >
                          {/* Author & Timestamp Header */}
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2.5">
                              {/* Initials Avatar */}
                              <div className="w-7 h-7 rounded-full bg-emerald-100 text-emerald-800 font-extrabold text-xs flex items-center justify-center select-none shrink-0">
                                {(comment.authorName || 'C').charAt(0).toUpperCase()}
                              </div>
                              <div className="flex items-center gap-2">
                                <span className="text-xs font-bold text-slate-800">
                                  {comment.authorName || 'Citizen'}
                                </span>
                                <span className="text-[11px] text-slate-400">
                                  {formatRelativeTime(comment.createdAt)}
                                </span>
                                {wasEdited && (
                                  <span className="text-[10px] text-slate-400 italic">
                                    (edited)
                                  </span>
                                )}
                              </div>
                            </div>

                            {/* Author Action Buttons */}
                            {isAuthor && !isEditing && !isDeleting && (
                              <div className="flex items-center gap-1">
                                {comment.editableByCurrentUser && (
                                  <button
                                    type="button"
                                    onClick={() => handleStartEdit(comment)}
                                    title="Edit comment"
                                    aria-label="Edit comment"
                                    className="p-1.5 text-slate-400 hover:text-emerald-700 hover:bg-emerald-50 rounded-lg transition-all cursor-pointer"
                                  >
                                    <PencilIcon className="w-3.5 h-3.5" />
                                  </button>
                                )}
                                {comment.deletableByCurrentUser && (
                                  <button
                                    type="button"
                                    onClick={() => setDeletingCommentId(comment.id)}
                                    title="Delete comment"
                                    aria-label="Delete comment"
                                    className="p-1.5 text-slate-400 hover:text-red-700 hover:bg-red-50 rounded-lg transition-all cursor-pointer"
                                  >
                                    <TrashCanIcon className="w-3.5 h-3.5" />
                                  </button>
                                )}
                              </div>
                            )}
                          </div>

                          {/* Inline Edit View */}
                          {isEditing ? (
                            <div className="space-y-2 pt-1">
                              <textarea
                                rows={2}
                                maxLength={1000}
                                value={editingText}
                                onChange={(e) => {
                                  setEditingText(e.target.value)
                                  if (updateError) setUpdateError('')
                                }}
                                className="w-full px-3 py-2 text-sm rounded-xl border border-emerald-400 focus:ring-2 focus:ring-emerald-500/20 focus:outline-none resize-y"
                              />
                              <div className="flex items-center justify-between text-xs">
                                <span className="text-slate-400">
                                  {editingText.length} / 1000
                                </span>
                                {updateError && (
                                  <span className="text-red-600 font-semibold">{updateError}</span>
                                )}
                              </div>
                              <div className="flex items-center justify-end gap-2 pt-1">
                                <button
                                  type="button"
                                  onClick={handleCancelEdit}
                                  disabled={isUpdatingComment}
                                  className="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-all cursor-pointer"
                                >
                                  Cancel
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleSaveEdit(comment.id)}
                                  disabled={isUpdatingComment || !editingText.trim()}
                                  className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold bg-emerald-700 hover:bg-emerald-800 text-white shadow-xs transition-all active:scale-95 cursor-pointer disabled:bg-slate-300"
                                >
                                  {isUpdatingComment ? (
                                    <>
                                      <Spinner size={12} className="text-white" />
                                      <span>Saving...</span>
                                    </>
                                  ) : (
                                    'Save'
                                  )}
                                </button>
                              </div>
                            </div>
                          ) : isDeleting ? (
                            /* Inline Deletion Confirmation View */
                            <div className="p-3 bg-red-50/80 rounded-xl border border-red-200 text-xs space-y-2">
                              <p className="font-semibold text-red-800">
                                Delete this comment permanently?
                              </p>
                              <div className="flex items-center justify-end gap-2">
                                <button
                                  type="button"
                                  onClick={() => setDeletingCommentId(null)}
                                  disabled={isDeletingComment}
                                  className="px-3 py-1.5 rounded-lg font-semibold text-slate-700 bg-white hover:bg-slate-100 border border-slate-200 transition-all cursor-pointer"
                                >
                                  Cancel
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleConfirmDelete(comment.id)}
                                  disabled={isDeletingComment}
                                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-bold bg-red-600 hover:bg-red-700 text-white transition-all active:scale-95 cursor-pointer"
                                >
                                  {isDeletingComment ? (
                                    <>
                                      <Spinner size={12} className="text-white" />
                                      <span>Deleting...</span>
                                    </>
                                  ) : (
                                    'Yes, Delete'
                                  )}
                                </button>
                              </div>
                            </div>
                          ) : (
                            /* Normal Comment Content (XSS safe via React text node) */
                            <p className="text-sm text-slate-700 whitespace-pre-wrap break-words leading-relaxed pl-1">
                              {comment.content}
                            </p>
                          )}
                        </div>
                      )
                    })
                  )}

                  {/* Load More Button */}
                  {hasMoreComments && (
                    <div className="pt-2 text-center">
                      <button
                        type="button"
                        onClick={handleLoadMore}
                        disabled={isLoadingMore}
                        className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-xl text-xs font-bold text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 transition-all cursor-pointer"
                      >
                        {isLoadingMore ? (
                          <>
                            <Spinner size={14} className="text-emerald-700" />
                            <span>Loading older comments...</span>
                          </>
                        ) : (
                          'Load more comments'
                        )}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  )
}
