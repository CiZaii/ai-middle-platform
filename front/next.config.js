/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    formats: ['image/avif', 'image/webp'],
    remotePatterns: [
      {
        protocol: 'https',
        hostname: '**',
      },
    ],
  },
  experimental: {
    optimizePackageImports: [
      'lucide-react',
      'date-fns',
      'react-markdown',
    ],
  },
  // 配置API路由重写，将前端请求代理到后端
  async rewrites() {
    return [
      {
        source: '/api/proxy/:path*',
        destination: 'http://localhost:8080/api/proxy/:path*',
      },
    ];
  },
  webpack: (config, { isServer }) => {
    // 解决 react-pdf 在服务端渲染时的 canvas 依赖问题
    if (isServer) {
      config.resolve.alias.canvas = false;
    }
    // 忽略 canvas 模块
    config.externals = [...(config.externals || []), { canvas: 'canvas' }];
    
    return config;
  },
};

module.exports = nextConfig;
