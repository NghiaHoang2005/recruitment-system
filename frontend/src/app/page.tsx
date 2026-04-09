import Link from 'next/link';

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24 bg-gradient-to-br from-indigo-50 to-white">
      <div className="text-center max-w-2xl">
        <h1 className="text-5xl font-extrabold tracking-tight text-gray-900 sm:text-6xl mb-6">
          <span className="text-indigo-600">Tuyển dụng thông minh</span> với AI
        </h1>
        <p className="mt-4 text-xl text-gray-600 mb-10">
          Kết nối ứng viên tài năng với các nhà tuyển dụng hàng đầu qua hệ thống phân tích và đề xuất tự động.
        </p>
        <div className="flex justify-center gap-4">
          <Link href="/login" className="px-6 py-3 bg-indigo-600 text-white rounded-md font-medium hover:bg-indigo-700 transition shadow-lg">
            Đăng nhập
          </Link>
          <Link href="/register" className="px-6 py-3 bg-white text-indigo-600 border border-indigo-200 rounded-md font-medium hover:bg-indigo-50 transition">
            Tạo tài khoản
          </Link>
        </div>
      </div>
    </main>
  );
}
