export default function ReportLoadingSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* 4 Summary Cards Skeleton */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="bg-white rounded-2xl p-4 sm:p-5 border border-slate-100 shadow-2xs space-y-3"
          >
            <div className="flex items-center justify-between">
              <div className="w-9 h-9 rounded-xl bg-slate-100" />
              <div className="w-8 h-7 rounded-lg bg-slate-100" />
            </div>
            <div className="space-y-1.5 pt-1">
              <div className="w-20 h-4 rounded bg-slate-100" />
              <div className="w-28 h-3 rounded bg-slate-50" />
            </div>
          </div>
        ))}
      </div>

      {/* Filter and Search Bar Skeleton */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="w-20 h-9 rounded-xl bg-slate-100" />
          ))}
        </div>
        <div className="w-full lg:w-64 h-9 rounded-xl bg-slate-100" />
      </div>

      {/* Report Cards Skeleton */}
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="bg-white rounded-2xl sm:rounded-3xl border border-slate-100 p-5 shadow-2xs flex flex-col sm:flex-row items-start gap-4"
          >
            <div className="w-full sm:w-44 h-40 sm:h-28 rounded-xl bg-slate-100 shrink-0" />
            <div className="flex-1 space-y-3 w-full">
              <div className="w-3/4 h-5 rounded bg-slate-100" />
              <div className="flex gap-2">
                <div className="w-24 h-5 rounded bg-slate-100" />
                <div className="w-20 h-5 rounded bg-slate-50" />
              </div>
              <div className="w-1/2 h-4 rounded bg-slate-50" />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

