declare module 'bpmn-js/lib/NavigatedViewer' {
  interface ViewerOptions {
    container?: HTMLElement | string;
    width?: string | number;
    height?: string | number;
    additionalModules?: any[];
  }

  interface ImportXMLResult {
    warnings: string[];
  }

  class NavigatedViewer {
    constructor(options?: ViewerOptions);
    importXML(xml: string): Promise<ImportXMLResult>;
    destroy(): void;
    get(serviceName: string): any;
    on(event: string, callback: (...args: any[]) => void): void;
    off(event: string, callback: (...args: any[]) => void): void;
  }

  export default NavigatedViewer;
}

declare module 'bpmn-js' {
  export { default } from 'bpmn-js/lib/NavigatedViewer';
}

declare module '*.css' {
  const content: Record<string, string>;
  export default content;
}
