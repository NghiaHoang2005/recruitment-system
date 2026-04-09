export default function CandidateDashboard() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Dashboard Ứng viên</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
          <h3 className="font-medium text-gray-500 text-sm">Việc đã nộp</h3>
          <p className="text-3xl font-bold mt-2">0</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
          <h3 className="font-medium text-gray-500 text-sm">CV Đã phân tích</h3>
          <p className="text-3xl font-bold mt-2 text-indigo-600">Trống</p>
        </div>
      </div>
      <div className="mt-8">
        <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-8 text-center flex flex-col items-center">
          <svg className="w-12 h-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
          <h3 className="text-lg font-medium text-gray-900 mb-2">Tải CV của bạn lên</h3>
          <p className="text-sm text-gray-500 mb-6 max-w-md">Hệ thống AI của chúng tôi sẽ tự động phân tích và trích xuất kỹ năng, kinh nghiệm của bạn để khớp với các công việc phù hợp nhất.</p>
          <button className="px-4 py-2 bg-indigo-600 text-white rounded-md text-sm font-medium hover:bg-indigo-700">Chọn file CV</button>
        </div>
      </div>
    </div>
  );
}
