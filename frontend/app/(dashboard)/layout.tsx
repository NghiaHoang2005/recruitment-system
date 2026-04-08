import Link from 'next/link';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <div className="flex items-center">
              <Link href="/" className="font-bold text-xl text-indigo-600">
                AI ATS
              </Link>
            </div>
            <div className="flex items-center gap-4">
              <button className="text-sm text-gray-500 hover:text-gray-900 border border-gray-300 px-4 py-2 rounded-md transition">
                Đăng xuất
              </button>
            </div>
          </div>
        </div>
      </header>
      
      <main className="flex-1 max-w-7xl w-full mx-auto sm:px-6 lg:px-8 py-8">
        {children}
      </main>
    </div>
  );
}
