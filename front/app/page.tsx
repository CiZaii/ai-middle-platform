"use client";

import { useEffect, useRef, useState } from "react";
import { LoginDialog } from "@/components/features/auth/login-dialog";

export default function HomePage() {
  const cursorGlowRef = useRef<HTMLDivElement>(null);
  const model3DRef = useRef<HTMLDivElement>(null);
  const [loginDialogOpen, setLoginDialogOpen] = useState(false);

  useEffect(() => {
    // 鼠标跟随效果
    const handleMouseMove = (e: MouseEvent) => {
      if (cursorGlowRef.current) {
        cursorGlowRef.current.style.left = e.clientX + "px";
        cursorGlowRef.current.style.top = e.clientY + "px";
      }
    };

    document.addEventListener("mousemove", handleMouseMove);

    // 动态加载外部脚本
    const scripts = [
      "https://cdn.jsdelivr.net/npm/particles.js@2.0.0/particles.min.js",
      "https://cdn.jsdelivr.net/npm/three@0.132.2/build/three.min.js",
      "https://cdn.jsdelivr.net/npm/echarts@5.4.2/dist/echarts.min.js",
    ];

    const loadScript = (src: string) => {
      return new Promise((resolve, reject) => {
        const script = document.createElement("script");
        script.src = src;
        script.onload = resolve;
        script.onerror = reject;
        document.body.appendChild(script);
      });
    };

    Promise.all(scripts.map(loadScript))
      .then(() => {
        initParticles();
        init3DScene();
        initCharts();
      })
      .catch((err) => console.error("加载脚本失败:", err));

    return () => {
      document.removeEventListener("mousemove", handleMouseMove);
    };
  }, []);

  const initParticles = () => {
    if (typeof window !== "undefined" && (window as any).particlesJS) {
      (window as any).particlesJS("particles-js", {
        particles: {
          number: {
            value: 80,
            density: {
              enable: true,
              value_area: 800,
            },
          },
          color: {
            value: ["#5E17EB", "#24E7E7", "#FF2D92"],
          },
          shape: {
            type: "circle",
            stroke: {
              width: 0,
              color: "#000000",
            },
          },
          opacity: {
            value: 0.5,
            random: true,
          },
          size: {
            value: 3,
            random: true,
          },
          line_linked: {
            enable: true,
            distance: 150,
            color: "#5E17EB",
            opacity: 0.2,
            width: 1,
          },
          move: {
            enable: true,
            speed: 1,
            direction: "none",
            random: true,
            straight: false,
            out_mode: "out",
            bounce: false,
          },
        },
        interactivity: {
          detect_on: "canvas",
          events: {
            onhover: {
              enable: true,
              mode: "grab",
            },
            onclick: {
              enable: true,
              mode: "push",
            },
            resize: true,
          },
          modes: {
            grab: {
              distance: 140,
              line_linked: {
                opacity: 0.8,
              },
            },
            push: {
              particles_nb: 3,
            },
          },
        },
        retina_detect: true,
      });
    }
  };

  const init3DScene = () => {
    if (
      typeof window !== "undefined" &&
      (window as any).THREE &&
      model3DRef.current
    ) {
      const THREE = (window as any).THREE;
      const container = model3DRef.current;
      const width = container.clientWidth;
      const height = container.clientHeight;

      const scene = new THREE.Scene();
      const camera = new THREE.PerspectiveCamera(70, width / height, 0.1, 1000);
      camera.position.z = 5;

      const renderer = new THREE.WebGLRenderer({
        alpha: true,
        antialias: true,
      });
      renderer.setSize(width, height);
      renderer.setClearColor(0x000000, 0);
      container.appendChild(renderer.domElement);

      const geometry = new THREE.IcosahedronGeometry(1.5, 2);
      const material = new THREE.MeshBasicMaterial({
        color: 0x5e17eb,
        wireframe: true,
        transparent: true,
        opacity: 0.7,
      });

      const brain = new THREE.Mesh(geometry, material);
      scene.add(brain);

      const pointLight = new THREE.PointLight(0x24e7e7, 1, 100);
      pointLight.position.set(5, 5, 5);
      scene.add(pointLight);

      const animate = () => {
        requestAnimationFrame(animate);
        brain.rotation.x += 0.005;
        brain.rotation.y += 0.01;
        renderer.render(scene, camera);
      };

      animate();

      const handleResize = () => {
        const width = container.clientWidth;
        const height = container.clientHeight;
        camera.aspect = width / height;
        camera.updateProjectionMatrix();
        renderer.setSize(width, height);
      };

      window.addEventListener("resize", handleResize);
    }
  };

  const initCharts = () => {
    if (typeof window !== "undefined" && (window as any).echarts) {
      const echarts = (window as any).echarts;

      // 图表1
      const chart1Element = document.getElementById("chart1");
      if (chart1Element) {
        const chart1 = echarts.init(chart1Element);
        chart1.setOption({
          grid: {
            left: "5%",
            right: "5%",
            top: "10%",
            bottom: "10%",
          },
          xAxis: {
            type: "category",
            data: ["v1", "v2", "v3", "v4", "v5", "v6"],
            axisLine: {
              lineStyle: {
                color: "rgba(255, 255, 255, 0.2)",
              },
            },
            axisLabel: {
              color: "rgba(255, 255, 255, 0.6)",
              fontSize: 10,
            },
          },
          yAxis: {
            type: "value",
            axisLine: {
              show: false,
            },
            axisTick: {
              show: false,
            },
            axisLabel: {
              color: "rgba(255, 255, 255, 0.6)",
              fontSize: 10,
            },
            splitLine: {
              lineStyle: {
                color: "rgba(255, 255, 255, 0.1)",
              },
            },
          },
          series: [
            {
              data: [45, 60, 78, 90, 89, 98],
              type: "line",
              smooth: true,
              symbol: "circle",
              symbolSize: 6,
              lineStyle: {
                width: 3,
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: "#5E17EB" },
                  { offset: 1, color: "#FF2D92" },
                ]),
              },
              itemStyle: {
                color: "#5E17EB",
                borderColor: "#fff",
                borderWidth: 1,
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: "rgba(94, 23, 235, 0.3)" },
                  { offset: 1, color: "rgba(94, 23, 235, 0)" },
                ]),
              },
            },
          ],
        });

        window.addEventListener("resize", () => chart1.resize());
      }

      // 图表2
      const chart2Element = document.getElementById("chart2");
      if (chart2Element) {
        const chart2 = echarts.init(chart2Element);
        chart2.setOption({
          radar: {
            indicator: [
              { name: "数据加密", max: 100 },
              { name: "访问控制", max: 100 },
              { name: "合规审计", max: 100 },
              { name: "威胁检测", max: 100 },
              { name: "恢复能力", max: 100 },
            ],
            splitArea: {
              areaStyle: {
                color: ["rgba(255, 255, 255, 0.03)"],
              },
            },
            axisLine: {
              lineStyle: {
                color: "rgba(255, 255, 255, 0.2)",
              },
            },
            splitLine: {
              lineStyle: {
                color: "rgba(255, 255, 255, 0.1)",
              },
            },
          },
          series: [
            {
              type: "radar",
              data: [
                {
                  value: [95, 88, 92, 85, 90],
                  name: "安全指标",
                  areaStyle: {
                    color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                      {
                        color: "rgba(255, 45, 146, 0.5)",
                        offset: 0,
                      },
                      {
                        color: "rgba(255, 45, 146, 0)",
                        offset: 1,
                      },
                    ]),
                  },
                  lineStyle: {
                    width: 2,
                    color: "#FF2D92",
                  },
                  itemStyle: {
                    color: "#FF2D92",
                  },
                },
              ],
            },
          ],
        });

        window.addEventListener("resize", () => chart2.resize());
      }
    }
  };

  return (
    <div className="relative w-full h-screen overflow-hidden bg-[#0A0A14] text-white">
      {/* 鼠标光晕 */}
      <div
        ref={cursorGlowRef}
        className="fixed w-5 h-5 rounded-full pointer-events-none z-[9999] mix-blend-screen"
        style={{
          background: "radial-gradient(circle, #5E17EB, transparent 70%)",
          transform: "translate(-50%, -50%)",
        }}
      />

      {/* 粒子背景 */}
      <div id="particles-js" className="fixed w-full h-full top-0 left-0 z-0" />

      {/* 3D模型容器 */}
      <div
        ref={model3DRef}
        id="model-container"
        className="absolute w-full h-full top-0 left-0 z-[1]"
      />

      {/* 主内容 */}
      <div className="relative z-10 w-full h-screen flex flex-col">
        {/* 导航栏 */}
        <nav className="backdrop-blur-md bg-white/5 border border-white/10 rounded-2xl py-4 px-6 md:px-12 flex justify-between items-center mb-8 mx-4 mt-4">
          <div className="flex items-center">
            <svg
              className="w-10 h-10 mr-2 text-indigo-500"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
            </svg>
            <h1
              className="text-xl md:text-2xl font-light tracking-wider"
              style={{
                textShadow:
                  "0 0 10px rgba(94, 23, 235, 0.7), 0 0 20px rgba(94, 23, 235, 0.5)",
              }}
            >
              AI<span className="text-indigo-400">NEXUS</span>
            </h1>
          </div>
          <div className="hidden md:flex space-x-8 text-sm font-light">
            <a href="#" className="hover:text-indigo-400 transition-colors">
              首页
            </a>
            <a href="#" className="hover:text-indigo-400 transition-colors">
              产品
            </a>
            <a href="#" className="hover:text-indigo-400 transition-colors">
              解决方案
            </a>
            <a href="#" className="hover:text-indigo-400 transition-colors">
              研究
            </a>
            <a href="#" className="hover:text-indigo-400 transition-colors">
              关于我们
            </a>
          </div>
          <button
            onClick={() => setLoginDialogOpen(true)}
            className="relative backdrop-blur-md bg-white/5 border border-white/10 rounded-lg px-4 py-2 text-sm font-light flex items-center hover:bg-white/10 transition-all cursor-pointer hover:scale-105"
          >
            <svg
              className="w-4 h-4 mr-2"
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path d="M5 4a1 1 0 00-2 0v7.268a2 2 0 000 3.464V16a1 1 0 102 0v-1.268a2 2 0 000-3.464V4zM11 4a1 1 0 10-2 0v1.268a2 2 0 000 3.464V16a1 1 0 102 0V8.732a2 2 0 000-3.464V4zM16 3a1 1 0 011 1v7.268a2 2 0 010 3.464V16a1 1 0 11-2 0v-1.268a2 2 0 010-3.464V4a1 1 0 011-1z" />
            </svg>
            开始体验
          </button>
        </nav>

        {/* 主要内容区 */}
        <main className="flex-1 px-6 md:px-12 grid grid-cols-1 lg:grid-cols-12 gap-8 pb-12 overflow-y-auto">
          {/* 左侧内容区 */}
          <div className="lg:col-span-5 flex flex-col justify-center">
            <div className="mb-8 animate-float-slow">
              <h2
                className="text-4xl md:text-6xl font-bold mb-6 leading-tight"
                style={{
                  textShadow:
                    "0 0 10px rgba(94, 23, 235, 0.7), 0 0 20px rgba(94, 23, 235, 0.5)",
                }}
              >
                未来人工智能
                <br />
                <span className="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 bg-clip-text text-transparent font-bold">
                  突破性体验
                </span>
              </h2>
              <p className="text-gray-300 mb-8 text-lg font-light leading-relaxed">
                采用前沿深度学习算法，打造全新一代智能生态系统，重新定义人机交互的未来形态，赋能各行业数字化转型。
              </p>
            </div>

            <div className="flex flex-wrap gap-4 mb-12">
              <button className="backdrop-blur-md bg-gradient-to-r from-indigo-600 to-purple-600 rounded-lg px-6 py-3 shadow-lg shadow-indigo-500/30 flex items-center hover:scale-105 transition-transform">
                <svg
                  className="w-5 h-5 mr-2"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M12.395 2.553a1 1 0 00-1.45-.385c-.345.23-.614.558-.822.88-.214.33-.403.713-.57 1.116-.334.804-.614 1.768-.84 2.734a31.365 31.365 0 00-.613 3.58 2.64 2.64 0 01-.945-1.067c-.328-.68-.398-1.534-.398-2.654A1 1 0 005.05 6.05 6.981 6.981 0 003 11a7 7 0 1011.95-4.95c-.592-.591-.98-.985-1.348-1.467-.363-.476-.724-1.063-1.207-2.03zM12.12 15.12A3 3 0 017 13s.879.5 2.5.5c0-1 .5-4 1.25-4.5.5 1 .786 1.293 1.371 1.879A2.99 2.99 0 0113 13a2.99 2.99 0 01-.879 2.121z"
                    clipRule="evenodd"
                  />
                </svg>
                探索技术
              </button>
              <button className="backdrop-blur-md bg-white/10 rounded-lg px-6 py-3 border border-indigo-500/40 flex items-center hover:scale-105 transition-transform">
                <svg
                  className="w-5 h-5 mr-2"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z"
                    clipRule="evenodd"
                  />
                </svg>
                技术白皮书
              </button>
            </div>

            {/* 数据统计 */}
            <div className="grid grid-cols-3 gap-4">
              <div className="backdrop-blur-md bg-white/5 border border-white/10 rounded-lg p-4 hover:scale-105 transition-transform">
                <span className="text-[#24E7E7] text-2xl font-semibold">
                  99.8%
                </span>
                <p className="text-xs text-gray-400">识别准确率</p>
              </div>
              <div className="backdrop-blur-md bg-white/5 border border-white/10 rounded-lg p-4 hover:scale-105 transition-transform">
                <span className="text-[#FF2D92] text-2xl font-semibold">
                  10TB+
                </span>
                <p className="text-xs text-gray-400">训练数据</p>
              </div>
              <div className="backdrop-blur-md bg-white/5 border border-white/10 rounded-lg p-4 hover:scale-105 transition-transform">
                <span className="text-indigo-400 text-2xl font-semibold">
                  500ms
                </span>
                <p className="text-xs text-gray-400">响应时间</p>
              </div>
            </div>
          </div>

          {/* 中间区域 - 留白呼吸区域 */}
          <div className="hidden lg:block lg:col-span-2"></div>

          {/* 右侧内容区 */}
          <div className="lg:col-span-5 flex flex-col justify-center space-y-6">
            {/* 特性卡片1 */}
            <div className="backdrop-blur-md bg-white/5 border border-white/10 rounded-lg p-6 hover:scale-105 transition-transform animate-float-fast">
              <div className="flex items-start mb-4">
                <svg
                  className="w-8 h-8 text-indigo-400 mr-4 flex-shrink-0"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M13 7H7v6h6V7z" />
                  <path
                    fillRule="evenodd"
                    d="M7 2a1 1 0 012 0v1h2V2a1 1 0 112 0v1h2a2 2 0 012 2v2h1a1 1 0 110 2h-1v2h1a1 1 0 110 2h-1v2a2 2 0 01-2 2h-2v1a1 1 0 11-2 0v-1H9v1a1 1 0 11-2 0v-1H5a2 2 0 01-2-2v-2H2a1 1 0 110-2h1V9H2a1 1 0 010-2h1V5a2 2 0 012-2h2V2zM5 5h10v10H5V5z"
                    clipRule="evenodd"
                  />
                </svg>
                <div>
                  <h3 className="text-xl font-light mb-2">神经网络架构</h3>
                  <p className="text-gray-300 text-sm">
                    采用最新的变换器模型，提供前所未有的自然语言理解能力
                  </p>
                </div>
              </div>
              <div className="w-full h-[200px]" id="chart1"></div>
            </div>

            {/* 特性卡片2 */}
            <div className="backdrop-blur-md bg-white/5 border border-white/10 rounded-lg p-6 hover:scale-105 transition-transform animate-float">
              <div className="flex items-start mb-4">
                <svg
                  className="w-8 h-8 text-[#24E7E7] mr-4 flex-shrink-0"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M11 3a1 1 0 10-2 0v1a1 1 0 102 0V3zM15.657 5.757a1 1 0 00-1.414-1.414l-.707.707a1 1 0 001.414 1.414l.707-.707zM18 10a1 1 0 01-1 1h-1a1 1 0 110-2h1a1 1 0 011 1zM5.05 6.464A1 1 0 106.464 5.05l-.707-.707a1 1 0 00-1.414 1.414l.707.707zM5 10a1 1 0 01-1 1H3a1 1 0 110-2h1a1 1 0 011 1zM8 16v-1h4v1a2 2 0 11-4 0zM12 14c.015-.34.208-.646.477-.859a4 4 0 10-4.954 0c.27.213.462.519.476.859h4.002z" />
                </svg>
                <div>
                  <h3 className="text-xl font-light mb-2">多模态交互</h3>
                  <p className="text-gray-300 text-sm">
                    融合文本、语音、视觉等多种模态，创建无缝智能交互体验
                  </p>
                </div>
              </div>
              <div className="flex mt-4 space-x-3">
                <div className="backdrop-blur-md bg-white/5 border border-white/10 rounded-lg p-3 flex-1 flex flex-col items-center">
                  <svg
                    className="w-6 h-6 text-gray-300 mb-2"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M7 2a1 1 0 011 1v1h3a1 1 0 110 2H9.578a18.87 18.87 0 01-1.724 4.78c.29.354.596.696.914 1.026a1 1 0 11-1.44 1.389c-.188-.196-.373-.396-.554-.6a19.098 19.098 0 01-3.107 3.567 1 1 0 01-1.334-1.49 17.087 17.087 0 003.13-3.733 18.992 18.992 0 01-1.487-2.494 1 1 0 111.79-.89c.234.47.489.928.764 1.372.417-.934.752-1.913.997-2.927H3a1 1 0 110-2h3V3a1 1 0 011-1zm6 6a1 1 0 01.894.553l2.991 5.982a.869.869 0 01.02.037l.99 1.98a1 1 0 11-1.79.895L15.383 16h-4.764l-.724 1.447a1 1 0 11-1.788-.894l.99-1.98.019-.038 2.99-5.982A1 1 0 0113 8zm-1.382 6h2.764L13 11.236 11.618 14z"
                      clipRule="evenodd"
                    />
                  </svg>
                  <span className="text-xs text-gray-400">自然语言</span>
                </div>
                <div className="backdrop-blur-md bg-white/5 border border-white/10 rounded-lg p-3 flex-1 flex flex-col items-center">
                  <svg
                    className="w-6 h-6 text-gray-300 mb-2"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M9.383 3.076A1 1 0 0110 4v12a1 1 0 01-1.707.707L4.586 13H2a1 1 0 01-1-1V8a1 1 0 011-1h2.586l3.707-3.707a1 1 0 011.09-.217zM14.657 2.929a1 1 0 011.414 0A9.972 9.972 0 0119 10a9.972 9.972 0 01-2.929 7.071 1 1 0 01-1.414-1.414A7.971 7.971 0 0017 10c0-2.21-.894-4.208-2.343-5.657a1 1 0 010-1.414zm-2.829 2.828a1 1 0 011.415 0A5.983 5.983 0 0115 10a5.984 5.984 0 01-1.757 4.243 1 1 0 01-1.415-1.415A3.984 3.984 0 0013 10a3.983 3.983 0 00-1.172-2.828 1 1 0 010-1.415z"
                      clipRule="evenodd"
                    />
                  </svg>
                  <span className="text-xs text-gray-400">语音识别</span>
                </div>
                <div className="backdrop-blur-md bg-white/5 border border-white/10 rounded-lg p-3 flex-1 flex flex-col items-center">
                  <svg
                    className="w-6 h-6 text-gray-300 mb-2"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M4 5a2 2 0 00-2 2v8a2 2 0 002 2h12a2 2 0 002-2V7a2 2 0 00-2-2h-1.586a1 1 0 01-.707-.293l-1.121-1.121A2 2 0 0011.172 3H8.828a2 2 0 00-1.414.586L6.293 4.707A1 1 0 015.586 5H4zm6 9a3 3 0 100-6 3 3 0 000 6z"
                      clipRule="evenodd"
                    />
                  </svg>
                  <span className="text-xs text-gray-400">视觉分析</span>
                </div>
              </div>
            </div>

            {/* 特性卡片3 */}
            <div className="backdrop-blur-md bg-white/5 border border-white/10 rounded-lg p-6 hover:scale-105 transition-transform animate-float-slow">
              <div className="flex items-start mb-4">
                <svg
                  className="w-8 h-8 text-[#FF2D92] mr-4 flex-shrink-0"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                    clipRule="evenodd"
                  />
                </svg>
                <div>
                  <h3 className="text-xl font-light mb-2">隐私与安全</h3>
                  <p className="text-gray-300 text-sm">
                    内置端到端加密与隐私保护机制，确保数据安全
                  </p>
                </div>
              </div>
              <div className="w-full h-[200px]" id="chart2"></div>
            </div>
          </div>
        </main>

        {/* 底部区域 */}
        <footer className="backdrop-blur-md bg-white/5 border border-white/10 rounded-t-2xl py-4 px-6 md:px-12 text-center text-gray-400 text-xs mx-4">
          <div className="mb-4">
            <div className="flex justify-center space-x-8 mb-4">
              <a href="#" className="hover:text-indigo-400 transition-colors">
                隐私政策
              </a>
              <a href="#" className="hover:text-indigo-400 transition-colors">
                使用条款
              </a>
              <a href="#" className="hover:text-indigo-400 transition-colors">
                联系我们
              </a>
            </div>
          </div>
          <p>© 2023 AINEXUS Tech. 下一代人工智能技术提供商</p>
        </footer>
      </div>

      <style jsx>{`
        @keyframes float {
          0% {
            transform: translateY(0px);
          }
          50% {
            transform: translateY(-15px);
          }
          100% {
            transform: translateY(0px);
          }
        }

        .animate-float {
          animation: float 6s ease-in-out infinite;
        }

        .animate-float-slow {
          animation: float 8s ease-in-out infinite;
        }

        .animate-float-fast {
          animation: float 4s ease-in-out infinite;
        }
      `}</style>

      {/* 登录弹窗 */}
      <LoginDialog open={loginDialogOpen} onOpenChange={setLoginDialogOpen} />
    </div>
  );
}