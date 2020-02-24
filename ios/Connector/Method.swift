import Foundation
import SwiftyJSON

typealias MethodFunction = (JSON?, Callback) -> Void

class Method {
  let function: MethodFunction
  
  init(_ function: @escaping MethodFunction) {
    self.function = function
  }
  
  func exec(_ data: JSON?, _ callback: Callback) {
    self.function(data, callback)
  }
}
