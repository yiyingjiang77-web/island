/**
 * 浏览器演示页使用的地图配置。
 *
 * 该文件使用普通 script，而不是 fetch JSON 或 ES module。这样从 file:// 直接打开
 * demo2.8-island.html 时也能读取配置，不会触发本地文件跨域限制。
 */
globalThis.MapConfig = Object.freeze({
  /**
   * 饮品店室外吧台布局（地图网格坐标）。
   *
   * 坐标说明：
   * - gx/gy 表示桌椅图片锚点所在的地图格坐标，允许使用小数做精细偏移。
   * - 当前饮品店小屋占据 gx=18..24、gy=31..36 附近区域；小屋不可进入。
   * - 1～3 号位于小屋左侧，4～6 号位于右侧，中间入口和正面通道必须留空。
   * - slotNumber 是服务端吧台槽位号，必须保持 1～6 唯一且不可随视觉排序改变。
   * - side 只允许 left/right，地图启动时会校验左右各三个。
   *
   * 替换图片时如何调整：
   * - 只改变图片外观且锚点、画布留白不变时，不需要修改 gx/gy。
   * - 图片尺寸、透明留白或锚点变化后，先调整 gx/gy，使桌椅贴近小屋两侧，
   *   再在 demo2.8-world.html 中同步检查绘制尺寸和 drinkBarRects 碰撞范围。
   * - 调整后必须确认六套桌椅互不重叠、不遮挡小屋入口，并逐个点击验证槽位号。
   */
  DRINK_BARS: Object.freeze([
    Object.freeze({slotNumber: 1, side: 'left',  gx: 16.7, gy: 31.6, state: 'EMPTY'}),
    Object.freeze({slotNumber: 2, side: 'left',  gx: 16.7, gy: 33.5, state: 'EMPTY'}),
    Object.freeze({slotNumber: 3, side: 'left',  gx: 16.7, gy: 35.4, state: 'EMPTY'}),
    Object.freeze({slotNumber: 4, side: 'right', gx: 25.3, gy: 31.6, state: 'EMPTY'}),
    Object.freeze({slotNumber: 5, side: 'right', gx: 25.3, gy: 33.5, state: 'EMPTY'}),
    Object.freeze({slotNumber: 6, side: 'right', gx: 25.3, gy: 35.4, state: 'EMPTY'}),
  ]),
});
