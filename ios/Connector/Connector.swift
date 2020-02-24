import Foundation
import SwiftyJSON

protocol Connector: class {
  
  var methodMap: [String : [Method]] { get set }
  
  func send(methodName: String)
  func send(methodName: String, data: JSON?)
  func send(methodName: String, callback: Callback)
  func send(methodName: String, data: JSON?, callback: Callback)
}

extension Connector {
  
  func runMethods(methodName: String, data: JSON?) {
    let methods = methodMap[methodName]
    if methods != nil {
      for method in methods! {
        method.exec(data, Callback { (callbackData: JSON?) -> Void in })
      }
    }
  }
  
  func runMethods(methodName: String, data: JSON?, sendKey: UInt) {
    let methods = methodMap[methodName]
    if methods != nil {
      for method in methods! {
        method.exec(data, Callback { (callbackData: JSON?) -> Void in
          self.send(methodName: "__CALLBACK_\(sendKey)", data: callbackData)
        })
      }
    }
  }
  
  func on(_ methodName: String, _ method: Method) {
    var methods = methodMap[methodName]
    if methods != nil {
      methods = []
    }
    methods!.append(method)
    methodMap[methodName] = methods
  }
  
  func off(_ methodName: String, _ method: Method) {
    var methods = methodMap[methodName]
    if methods != nil {
      methods!.removeAll(where: {$0 === method})
    }
  }
  
  func off(_ methodName: String) {
    methodMap.removeValue(forKey: methodName);
  }
}
