'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

export default function CreateJobPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    requirements: '',
    location: '',
    salaryRange: '',
    companyName: ''
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    // TODO: Khi nào fetch Auth Token thì sẽ kết nối tới api Spring Boot thực
    console.log('Submitting:', formData);
    setTimeout(() => {
      setLoading(false);
      router.push('/recruiter');
    }, 1000);
  };

  return (
    <div className="max-w-3xl mx-auto p-6 bg-white rounded-lg shadow-sm border border-gray-100">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Đăng tin tuyển dụng mới</h1>
        <Link href="/recruiter" className="text-sm text-gray-500 hover:text-gray-900 border border-gray-200 px-3 py-1.5 rounded-md transition">
          Quay lại
        </Link>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-2 md:col-span-2">
            <label className="text-sm font-medium">Tiêu đề công việc <span className="text-red-500">*</span></label>
            <input required name="title" value={formData.title} onChange={handleChange} className="w-full px-3 py-2 border border-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="VD: Senior Frontend Developer" />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium">Tên công ty <span className="text-red-500">*</span></label>
            <input required name="companyName" value={formData.companyName} onChange={handleChange} className="w-full px-3 py-2 border border-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="VD: Tech Corp" />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium">Địa điểm làm việc</label>
            <input name="location" value={formData.location} onChange={handleChange} className="w-full px-3 py-2 border border-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="VD: TP. Hồ Chí Minh" />
          </div>

          <div className="space-y-2 md:col-span-2">
            <label className="text-sm font-medium">Mức lương (Salary Range)</label>
            <input name="salaryRange" value={formData.salaryRange} onChange={handleChange} className="w-full px-3 py-2 border border-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="VD: $1000 - $2000 hoặc Thỏa thuận" />
          </div>

          <div className="space-y-2 md:col-span-2">
            <label className="text-sm font-medium">Mô tả công việc <span className="text-red-500">*</span></label>
            <textarea required name="description" value={formData.description} onChange={handleChange} rows={4} className="w-full px-3 py-2 border border-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="Chi tiết về các nhiệm vụ và trách nhiệm..."></textarea>
          </div>

          <div className="space-y-2 md:col-span-2">
            <label className="text-sm font-medium">Yêu cầu ứng viên <span className="text-red-500">*</span></label>
            <textarea required name="requirements" value={formData.requirements} onChange={handleChange} rows={4} className="w-full px-3 py-2 border border-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="Kỹ năng, kinh nghiệm cần thiết để đảm nhận vị trí..."></textarea>
          </div>
        </div>

        <div className="pt-4 flex justify-end">
          <button type="submit" disabled={loading} className="px-6 py-2 bg-indigo-600 text-white rounded-md font-medium hover:bg-indigo-700 disabled:opacity-50 transition">
            {loading ? 'Đang tạo...' : 'Đăng tin ngay'}
          </button>
        </div>
      </form>
    </div>
  );
}
