export default function LoginPage() {
  return (
    <div className="flex h-screen w-full items-center justify-center bg-gray-50">
      <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-md border border-gray-100">
        <h1 className="text-2xl font-bold text-center mb-6 text-indigo-700">Đăng nhập tài khoản</h1>
        <p className="text-sm text-center text-gray-500 mb-8">Xin chào, vui lòng nhập thông tin để truy cập hệ thống.</p>
        
        {/* Placeholder for Form */}
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1">Email</label>
            <input type="email" className="w-full border rounded-md px-3 py-2 text-sm" placeholder="user@gmail.com" />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Mật khẩu</label>
            <input type="password" className="w-full border rounded-md px-3 py-2 text-sm" placeholder="********" />
          </div>
          <button className="w-full bg-indigo-600 text-white py-2 rounded-md font-medium text-sm hover:bg-indigo-700 transition">Đăng nhập</button>
        </div>
      </div>
    </div>
  );
}
