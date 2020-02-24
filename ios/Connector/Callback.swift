import Foundation
import SwiftyJSON

typealias CallbackFunction = (JSON?) -> Void

class Callback {
  let function: CallbackFunction
  
  init(_ function: @escaping CallbackFunction) {
    self.function = function
  }
  
  func exec(_ data: JSON?) {
    self.function(data)
  }
}
