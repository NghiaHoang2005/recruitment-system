export default function RecruiterDashboard() {
  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Quản lý Việc làm</h1>
        <button className="px-4 py-2 bg-indigo-600 text-white rounded-md text-sm font-medium hover:bg-indigo-700">
          + Đăng tin mới
        </button>
      </div>
      
      <div className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-16 text-center text-gray-500">
          Chưa có tin tuyển dụng nào được đăng.
        </div>
      </div>
    </div>
  );
}
