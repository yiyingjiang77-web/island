/**
 * Cocos Creator 3.8 最小类型声明桩
 * 仅用于命令行 TypeScript 编译检查
 */

declare module 'cc' {
  export const _decorator: {
    ccclass: (name?: string) => ClassDecorator;
    property: (options?: any) => PropertyDecorator;
  };

  export class Component {
    node: Node;
    onLoad(): void;
    start(): void;
    update(dt: number): void;
    onDestroy(): void;
    destroy(): void;
    getComponent<T extends Component>(type: { new(...args: any[]): T }): T | null;
    addComponent<T extends Component>(type: { new(...args: any[]): T }): T;
  }

  export class Node {
    static EventType: {
      TOUCH_END: string;
    };
    name: string;
    parent: Node | null;
    children: Node[];
    active: boolean;
    position: Vec3;
    scale: Vec3;
    constructor(name?: string);
    setPosition(x: number, y: number, z: number): void;
    setPosition(position: Vec3): void;
    addChild(child: Node): void;
    removeFromParent(): void;
    destroy(): void;
    getChildByName(name: string): Node | null;
    getComponent<T extends Component>(type: { new(...args: any[]): T }): T | null;
    addComponent<T extends Component>(type: { new(...args: any[]): T }): T;
    on(eventType: string, callback: Function, target?: any): void;
    off(eventType: string, callback: Function, target?: any): void;
  }

  export class Vec3 {
    x: number; y: number; z: number;
    constructor(x?: number, y?: number, z?: number);
    static distance(a: Vec3, b: Vec3): number;
    static lerp(a: Vec3, b: Vec3, t: number): Vec3;
    clone(): Vec3;
    subtract(other: Vec3): Vec3;
    normalize(): Vec3;
    multiplyScalar(s: number): Vec3;
  }

  export class Vec2 {
    x: number; y: number;
    constructor(x?: number, y?: number);
    static distance(a: Vec2, b: Vec2): number;
    clone(): Vec2;
  }

  export class Color {
    r: number; g: number; b: number; a: number;
    constructor(r?: number, g?: number, b?: number, a?: number);
    static fromHEX(hex: string): Color;
  }

  export class Size {
    width: number; height: number;
    constructor(w?: number, h?: number);
  }

  export class Rect {
    x: number; y: number; width: number; height: number;
  }

  // ---- Director ----
  export const director: {
    loadScene(name: string): void;
    addPersistRootNode(node: Node): void;
    getScene(): Scene;
  };

  export class Scene {
    name: string;
  }

  // ---- Camera ----
  export class Camera extends Component {
    screenToWorld(screenPos: Vec3): Vec3;
  }

  // ---- Input ----
  export class EventTouch {
    getUILocation(): Vec2;
    getLocation(): Vec2;
  }

  export const input: {
    on(eventType: string, callback: Function, target?: any): void;
    off(eventType: string, callback: Function, target?: any): void;
  };

  export namespace Input {
    export const EventType: {
      TOUCH_START: string;
      TOUCH_MOVE: string;
      TOUCH_END: string;
    };
  }

  // ---- UI ----
  export class Label extends Component {
    string: string;
    fontSize: number;
    lineHeight: number;
    color: Color;
  }
  export class Sprite extends Component {
    color: Color;
    spriteFrame: any;
  }
  export class UITransform extends Component {
    width: number;
    height: number;
    setContentSize(w: number, h: number): void;
  }
  export class Canvas extends Component {}
  export class Widget extends Component {}

  // ---- Graphics ----
  export class Graphics extends Component {
    fillColor: Color;
    lineWidth: number;
    strokeColor: Color;
    rect(x: number, y: number, w: number, h: number): void;
    circle(cx: number, cy: number, r: number): void;
    moveTo(x: number, y: number): void;
    lineTo(x: number, y: number): void;
    close(): void;
    fill(): void;
    stroke(): void;
    clear(): void;
  }

  // ---- Tween ----
  export function tween(target: any): Tween;
  export class Tween {
    to(duration: number, props: any, opts?: any): Tween;
    by(duration: number, props: any, opts?: any): Tween;
    call(callback: Function): Tween;
    union(): Tween;
    repeatForever(): Tween;
    start(): Tween;
    stop(): Tween;
  }

  // ---- System ----
  export const sys: {
    localStorage: {
      getItem(key: string): string;
      setItem(key: string, value: string): void;
      removeItem(key: string): void;
    };
  };

  // ---- Utilities ----
  export function find(path: string): Node | null;
  export function instantiate(original: Node | Prefab): Node;

  export class Prefab {
    name: string;
    data: Node;
  }

  export const view: {
    getVisibleSize(): Size;
    getDesignResolutionSize(): Size;
    getFrameSize(): Size;
  };
}
