export default function AdminDashboard() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Quản trị Hệ thống</h1>
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
          <h3 className="font-medium text-gray-500 text-sm">Tổng User</h3>
          <p className="text-3xl font-bold mt-2">0</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
          <h3 className="font-medium text-gray-500 text-sm">Tin đang active</h3>
          <p className="text-3xl font-bold mt-2 text-indigo-600">0</p>
        </div>
      </div>
      <div className="mt-8 bg-white rounded-lg shadow-sm border border-gray-100 p-8 text-center text-gray-500">
        Tính năng duyệt và quản lý tài khoản đang được phát triển...
      </div>
    </div>
  );
}
