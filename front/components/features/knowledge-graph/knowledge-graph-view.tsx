'use client';

import { useEffect, useRef } from 'react';
import * as d3 from 'd3';

interface KnowledgeGraphNode {
  id: string;
  label: string;
  type?: string;
}

interface KnowledgeGraphEdge {
  source: string;
  target: string;
  label?: string;
}

interface KnowledgeGraphData {
  nodes: KnowledgeGraphNode[];
  edges: KnowledgeGraphEdge[];
}

interface KnowledgeGraphViewProps {
  graphData?: KnowledgeGraphData;
}

// 生成随机颜色的函数
const generateRandomColor = (seed: string): { fill: string; stroke: string } => {
  // 使用字符串哈希生成稳定的随机色相
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = seed.charCodeAt(i) + ((hash << 5) - hash);
  }
  const hue = Math.abs(hash % 360);
  
  // 生成明亮的填充色和稍深的边框色
  const fill = `hsl(${hue}, 70%, 65%)`;
  const stroke = `hsl(${hue}, 75%, 45%)`;
  
  return { fill, stroke };
};

// 存储已生成的颜色，确保同一类型颜色一致
const typeColorCache: Record<string, { fill: string; stroke: string }> = {};

// 获取节点类型的颜色（同一类型返回相同颜色）
const getNodeTypeColor = (type: string): { fill: string; stroke: string } => {
  if (!typeColorCache[type]) {
    typeColorCache[type] = generateRandomColor(type);
  }
  return typeColorCache[type];
};

export function KnowledgeGraphView({ graphData }: KnowledgeGraphViewProps) {
  const svgRef = useRef<SVGSVGElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!graphData || !svgRef.current || !containerRef.current) return;

    const container = containerRef.current;
    const width = container.clientWidth;
    const height = 600;

    // 清空之前的内容
    d3.select(svgRef.current).selectAll('*').remove();

    const svg = d3
      .select(svgRef.current)
      .attr('width', width)
      .attr('height', height)
      .attr('viewBox', [0, 0, width, height]);

    // 添加缩放行为
    const g = svg.append('g');

    const zoom = d3.zoom<SVGSVGElement, unknown>()
      .scaleExtent([0.1, 4])
      .on('zoom', (event) => {
        g.attr('transform', event.transform);
      });

    svg.call(zoom);

    // 过滤掉文档节点（Document类型），只保留实体节点
    const filteredNodes = graphData.nodes.filter((node) => node.type !== 'Document');
    const documentNodeIds = new Set(
      graphData.nodes.filter((node) => node.type === 'Document').map((node) => node.id)
    );
    
    // 过滤掉与文档节点相关的边
    const filteredEdges = graphData.edges.filter(
      (edge) => !documentNodeIds.has(edge.source) && !documentNodeIds.has(edge.target)
    );

    // 转换数据格式
    const nodes = filteredNodes.map((d) => ({ ...d }));
    const links = filteredEdges.map((d) => ({ ...d }));

    // 创建力导向图模拟
    const simulation = d3
      .forceSimulation(nodes as any)
      .force(
        'link',
        d3
          .forceLink(links as any)
          .id((d: any) => d.id)
          .distance(150)
      )
      .force('charge', d3.forceManyBody().strength(-800))
      .force('center', d3.forceCenter(width / 2, height / 2))
      .force('collision', d3.forceCollide().radius(60));

    // 添加箭头标记
    const defs = svg.append('defs');
    
    // 收集所有节点类型并为每种类型创建箭头
    const nodeTypes = Array.from(new Set(nodes.map((n: any) => n.type || 'default')));
    nodeTypes.forEach((type) => {
      const colors = getNodeTypeColor(type);
      defs
        .append('marker')
        .attr('id', `arrow-${type}`)
        .attr('viewBox', '0 -5 10 10')
        .attr('refX', 35)
        .attr('refY', 0)
        .attr('markerWidth', 6)
        .attr('markerHeight', 6)
        .attr('orient', 'auto')
        .append('path')
        .attr('d', 'M0,-5L10,0L0,5')
        .attr('fill', colors.stroke);
    });

    // 创建连线
    const link = g
      .append('g')
      .attr('class', 'links')
      .selectAll('line')
      .data(links)
      .join('line')
      .attr('stroke', '#8b5cf6')
      .attr('stroke-opacity', 0.6)
      .attr('stroke-width', 2)
      .attr('marker-end', (d: any) => {
        const target = nodes.find((n) => n.id === d.target);
        const type = target?.type || 'default';
        return `url(#arrow-${type})`;
      });

    // 创建连线标签
    const linkLabel = g
      .append('g')
      .attr('class', 'link-labels')
      .selectAll('text')
      .data(links)
      .join('text')
      .attr('class', 'link-label')
      .attr('text-anchor', 'middle')
      .attr('font-size', '12px')
      .attr('fill', '#6b7280')
      .attr('font-weight', '500')
      .text((d: any) => d.label || '');

    // 创建节点容器
    const node = g
      .append('g')
      .attr('class', 'nodes')
      .selectAll('g')
      .data(nodes)
      .join('g')
      .attr('class', 'node')
      .call(
        d3
          .drag<any, any>()
          .on('start', dragstarted)
          .on('drag', dragged)
          .on('end', dragended)
      );

    // 添加节点圆形
    node
      .append('circle')
      .attr('r', 30)
      .attr('fill', (d: any) => {
        const colors = getNodeTypeColor(d.type || 'default');
        return colors.fill;
      })
      .attr('stroke', (d: any) => {
        const colors = getNodeTypeColor(d.type || 'default');
        return colors.stroke;
      })
      .attr('stroke-width', 3)
      .attr('filter', 'drop-shadow(0 4px 6px rgba(0, 0, 0, 0.1))')
      .style('cursor', 'pointer');

    // 添加节点标签
    node
      .append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', '0.35em')
      .attr('font-size', '12px')
      .attr('font-weight', '600')
      .attr('fill', 'white')
      .attr('pointer-events', 'none')
      .text((d: any) => {
        // 如果标签太长，截断并添加省略号
        const maxLength = 8;
        return d.label.length > maxLength
          ? d.label.substring(0, maxLength) + '...'
          : d.label;
      });

    // 添加完整标签的 tooltip
    node.append('title').text((d: any) => d.label);

    // 添加悬停效果
    node
      .on('mouseenter', function () {
        d3.select(this).select('circle').attr('r', 35).attr('stroke-width', 4);
      })
      .on('mouseleave', function () {
        d3.select(this).select('circle').attr('r', 30).attr('stroke-width', 3);
      });

    // 更新位置
    simulation.on('tick', () => {
      link
        .attr('x1', (d: any) => d.source.x)
        .attr('y1', (d: any) => d.source.y)
        .attr('x2', (d: any) => d.target.x)
        .attr('y2', (d: any) => d.target.y);

      linkLabel
        .attr('x', (d: any) => (d.source.x + d.target.x) / 2)
        .attr('y', (d: any) => (d.source.y + d.target.y) / 2);

      node.attr('transform', (d: any) => `translate(${d.x},${d.y})`);
    });

    // 拖拽事件处理
    function dragstarted(event: any) {
      if (!event.active) simulation.alphaTarget(0.3).restart();
      event.subject.fx = event.subject.x;
      event.subject.fy = event.subject.y;
    }

    function dragged(event: any) {
      event.subject.fx = event.x;
      event.subject.fy = event.y;
    }

    function dragended(event: any) {
      if (!event.active) simulation.alphaTarget(0);
      event.subject.fx = null;
      event.subject.fy = null;
    }

    // 清理函数
    return () => {
      simulation.stop();
    };
  }, [graphData]);

  // 过滤掉文档节点，只显示实体节点
  const filteredNodes = graphData?.nodes.filter((node) => node.type !== 'Document') || [];
  
  if (!graphData || filteredNodes.length === 0) {
    return (
      <div className="h-[600px] flex items-center justify-center bg-muted/30 rounded-lg">
        <p className="text-muted-foreground">暂无知识图谱数据</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* 操作提示和图例 */}
      <div className="flex items-center justify-between gap-4">
        <div className="bg-white dark:bg-slate-800 rounded-lg p-3 shadow-sm border">
          <div className="text-xs font-semibold mb-2 text-slate-700 dark:text-slate-300">节点类型</div>
          <div className="flex flex-wrap gap-3">
            {Array.from(new Set(filteredNodes.map((node) => node.type || 'default'))).map((type) => {
              const colors = getNodeTypeColor(type);
              const typeLabels: Record<string, string> = {
                Person: '人物',
                Organization: '组织',
                Product: '产品',
                Technology: '技术',
                Project: '项目',
                Skill: '技能',
                Concept: '概念',
                default: '默认',
              };
              return (
                <div key={type} className="flex items-center gap-2">
                  <div
                    className="w-3 h-3 rounded-full"
                    style={{
                      backgroundColor: colors.fill,
                      border: `2px solid ${colors.stroke}`,
                    }}
                  />
                  <span className="text-xs text-slate-600 dark:text-slate-400">
                    {typeLabels[type] || type}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-lg p-3 shadow-sm border">
          <div className="text-xs space-y-1 text-slate-600 dark:text-slate-400">
            <div>🖱️ 拖拽节点移动</div>
            <div>🔍 滚轮缩放</div>
            <div>✋ 拖拽画布平移</div>
          </div>
        </div>
      </div>

      {/* 知识图谱画布 */}
      <div ref={containerRef} className="h-[600px] border rounded-lg overflow-hidden bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800">
        <svg ref={svgRef} className="w-full h-full" />
      </div>
    </div>
  );
}
