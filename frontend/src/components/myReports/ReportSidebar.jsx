import { useNavigate } from 'react-router-dom'
import {
  ChartBarIcon,
  CheckCircleIcon,
  DocumentIcon,
  QuestionMarkCircleIcon,
  LeafLogo,
  ArrowRightIcon,
} from '../home/HomeIcons.jsx'

export default function ReportSidebar({ complaints = [] }) {
  const navigate = useNavigate()

  const totalReports = complaints.length
  const resolvedReports = complaints.filter(
    (c) => c.status === 'RESOLVED' || c.status === 'CLOSED'
  ).length
  const activeReports = complaints.filter(
    (c) => c.status === 'SUBMITTED' || c.status === 'IN_PROGRESS'
  ).length

  return (
    <aside className="w-full lg:w-80 space-y-6 shrink-0">
      {/* 1. Your Impact Card */}
      <div className="bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-6 shadow-2xs space-y-5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-700 flex items-center justify-center shrink-0 border border-emerald-100">
            <ChartBarIcon className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-slate-900 tracking-tight leading-snug">
              Your Impact
            </h3>
            <p className="text-xs text-slate-400 font-medium">
              You're making a real difference
            </p>
          </div>
        </div>

        <div className="space-y-3.5 pt-1">
          {/* Stat 1: Total Reports */}
          <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
            <div className="flex items-center gap-2.5">
              <DocumentIcon className="w-4 h-4 text-slate-500" />
              <span className="text-xs font-bold text-slate-700">Total Submitted</span>
            </div>
            <span className="text-base font-extrabold text-slate-900">
              {totalReports}
            </span>
          </div>

          {/* Stat 2: Issues Resolved */}
          <div className="flex items-center justify-between p-3 rounded-xl bg-emerald-50/50 border border-emerald-100">
            <div className="flex items-center gap-2.5">
              <CheckCircleIcon className="w-4 h-4 text-emerald-600" />
              <span className="text-xs font-bold text-emerald-900">Issues Resolved</span>
            </div>
            <span className="text-base font-extrabold text-emerald-700">
              {resolvedReports}
            </span>
          </div>

          {/* Stat 3: Active Reports */}
          <div className="flex items-center justify-between p-3 rounded-xl bg-blue-50/40 border border-blue-100">
            <div className="flex items-center gap-2.5">
              <div className="w-2 h-2 rounded-full bg-blue-500 animate-pulse" />
              <span className="text-xs font-bold text-blue-900">Active in Review</span>
            </div>
            <span className="text-base font-extrabold text-blue-800">
              {activeReports}
            </span>
          </div>
        </div>
      </div>

      {/* 2. Need Help / Guidelines Card */}
      <div className="bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-6 shadow-2xs space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-indigo-50 text-indigo-700 flex items-center justify-center shrink-0 border border-indigo-100">
            <QuestionMarkCircleIcon className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-slate-900 tracking-tight leading-snug">
              Need Help?
            </h3>
            <p className="text-xs text-slate-400 font-medium">
              Guidelines & civic reporting tips
            </p>
          </div>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed">
          Questions about resolution timelines or how duplicate detection works? Check our community guidelines.
        </p>

        <div className="space-y-2 pt-1">
          <button
            type="button"
            onClick={() => navigate('/guidelines')}
            className="w-full py-2.5 px-4 rounded-xl text-xs font-bold text-slate-700 hover:text-emerald-700 bg-slate-50 hover:bg-emerald-50/60 border border-slate-200 hover:border-emerald-200 transition-all flex items-center justify-between cursor-pointer"
          >
            <span>View Civic Guidelines</span>
            <ArrowRightIcon className="w-3.5 h-3.5" />
          </button>

          <button
            type="button"
            onClick={() => navigate('/report-issue')}
            className="w-full py-2.5 px-4 rounded-xl text-xs font-bold text-emerald-800 hover:text-white bg-emerald-50 hover:bg-emerald-700 border border-emerald-200 hover:border-emerald-700 transition-all flex items-center justify-between cursor-pointer"
          >
            <span>Report Another Issue</span>
            <ArrowRightIcon className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* 3. Civic Quote Card */}
      <div className="bg-gradient-to-br from-emerald-50/80 to-teal-50/40 rounded-2xl sm:rounded-3xl border border-emerald-100/80 p-6 shadow-2xs relative overflow-hidden">
        <div className="flex items-center gap-2 mb-3">
          <LeafLogo className="w-6 h-6" />
          <span className="text-xs font-extrabold text-emerald-800 uppercase tracking-wider">
            CivicSense Mission
          </span>
        </div>
        <blockquote className="text-xs italic text-slate-700 font-medium leading-relaxed">
          "Small actions by informed citizens create cleaner, safer and happier cities."
        </blockquote>
        <p className="text-[11px] font-bold text-emerald-700 mt-2">
          — CivicSense Community
        </p>
      </div>
    </aside>
  )
}

