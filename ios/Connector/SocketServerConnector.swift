import Foundation
import SwiftyJSON

typealias ConnectionListener = () -> Void
typealias ConnectionFailedHandler = () -> Void

class SocketServerConnector: NSObject, Connector, StreamDelegate {
  
  var methodMap: [String : [Method]] = [:]
  var sendKey: UInt = 0
  var isConnected: Bool = false
  
  let host: String
  let port: Int
  let connectionListener: ConnectionListener
  let connectionFailedHandler: ConnectionFailedHandler
  
  var inputStream: InputStream?
  var outputStream: OutputStream?
  var connectionTimeoutTimer: Timer?
  var totalStr = ""
  
  func disconnect() {
    
    inputStream?.close()
    outputStream?.close()
    connectionTimeoutTimer?.invalidate()
    
    inputStream = nil
    outputStream = nil
    connectionTimeoutTimer = nil
    
    isConnected = false
    runMethods(methodName: "disconnect", data: JSON())
  }
  
  func reconnect() {
    disconnect()
    
    Stream.getStreamsToHost(
      withName: host, port: port,
      inputStream: &inputStream, outputStream: &outputStream
    )
    
    if inputStream != nil && outputStream != nil {
      
      inputStream!.delegate = self
      outputStream!.delegate = self
      inputStream!.schedule(in: .main, forMode: RunLoop.Mode.default)
      outputStream!.schedule(in: .main, forMode: RunLoop.Mode.default)
      inputStream!.open()
      outputStream!.open()
      
      DispatchQueue.main.async {
        
        self.connectionTimeoutTimer = Timer.scheduledTimer(withTimeInterval: 3, repeats: false, block: { _ in
          if self.isConnected != true {
            print("Failed to connect to server.")
            self.disconnect()
          } else {
            self.connectionTimeoutTimer?.invalidate()
            self.connectionTimeoutTimer = nil
          }
        })
      }
      
    } else {
      print("Failed to connect to server.")
      disconnect()
    }
  }
  
  func stream(_ stream: Stream, handle eventCode: Stream.Event) {
    
    if stream == inputStream {
      
      switch eventCode {
        
        case .openCompleted:
          isConnected = true
          print("Server \(host) connected")
          connectionListener()
          break
        
        case .hasBytesAvailable:
          
          let bufferSize = 1024
          let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
          
          while inputStream!.hasBytesAvailable {
            
            let count = inputStream!.read(buffer, maxLength: bufferSize)
            if count < 0 {
              let error = inputStream!.streamError! as NSError
              print("Input stream error \(error.domain) / \(error.code)")
              disconnect()
              break
            } else if count == 0 {
              break
            }
            
            let data = Data(bytes: buffer, count: count)
            let part = String(data: data, encoding: String.Encoding.utf8)!
            
            let found = part.lastIndex(of: "\r\n")
            if found == nil {
              totalStr += part
            } else {
              
              let params = JSON(totalStr + part[..<found!])
              
              let methodName = params["methodName"].string!
              
              var data: JSON?
              if params["data"].exists() == true {
                data = params["data"]
              }
              
              if params["sendKey"].exists() == true {
                runMethods(methodName: methodName, data: data)
              } else {
                runMethods(methodName: methodName, data: data, sendKey: params["sendKey"].uInt!)
              }
            }
          }
          
          buffer.deallocate()
          break
        
        case .hasSpaceAvailable:
          break
        
        case .endEncountered:
          print("Input stream ended")
          disconnect()
          break
        
        case .errorOccurred:
          
          let error = stream.streamError! as NSError
          print("Input stream error \(error.domain) / \(error.code)")
          
          if isConnected == true {
            disconnect()
          } else {
            print("Failed to connect to server.")
            disconnect()
          }
          break
        
        default:
          fatalError()
          break
      }
    }
    
    else if stream == outputStream {
      
      switch eventCode {
        
        case .openCompleted:
          print("Output stream did open")
          break
        
        case .hasBytesAvailable:
          break
        
        case .hasSpaceAvailable:
          break
        
        case .endEncountered:
          print("Output stream end")
          break
        
        case .errorOccurred:
          
          let error = stream.streamError! as NSError
          print("Output stream error \(error.domain) / \(error.code)")
          break
        
        default:
          fatalError()
          break
      }
    }
  }
  
  init(
    host: String, port: Int,
    connectionListener: @escaping ConnectionListener,
    connectionFailedHandler: @escaping ConnectionFailedHandler
  ) {
    self.host = host
    self.port = port
    self.connectionListener = connectionListener
    self.connectionFailedHandler = connectionFailedHandler
    
    super.init()
    reconnect()
  }
  
  func send(methodName: String) {
    if isConnected == true {
      
      var sendData = JSON()
      sendData["methodName"].string = methodName
      
      let json = sendData.rawString([.castNilToNSNull: true])!
      outputStream!.write(json, maxLength: json.utf8.count)
      
    } else {
      print("Socket not connected.")
    }
  }
  
  func send(methodName: String, data: JSON?) {
    if isConnected == true {
      
      var sendData = JSON()
      sendData["methodName"].string = methodName
      if (data != nil) {
        sendData["data"] = data!
      }
      
      let json = sendData.rawString([.castNilToNSNull: true])!
      outputStream!.write(json, maxLength: json.utf8.count)
      
    } else {
      print("Socket not connected.")
    }
  }
  
  func send(methodName: String, callback: Callback) {
    if isConnected == true {
      
      let callbackName = "__CALLBACK_\(sendKey)"
      on(callbackName, Method { (callbackData: JSON?, _) -> Void in
        callback.exec(callbackData)
        self.off(callbackName)
      })
      
      var sendData = JSON()
      sendData["methodName"].string = methodName
      sendData["sendKey"].uInt = sendKey
      
      let json = sendData.rawString([.castNilToNSNull: true])!
      outputStream!.write(json, maxLength: json.utf8.count)
      
      sendKey += 1
      if sendKey >= UInt.max {
        sendKey = 0
      }
      
    } else {
      print("Socket not connected.")
    }
  }
  
  func send(methodName: String, data: JSON?, callback: Callback) {
    if isConnected == true {
      
      let callbackName = "__CALLBACK_\(sendKey)"
      on(callbackName, Method { (callbackData: JSON?, _) -> Void in
        callback.exec(callbackData)
        self.off(callbackName)
      })
      
      var sendData = JSON()
      sendData["methodName"].string = methodName
      if (data != nil) {
        sendData["data"] = data!
      }
      sendData["sendKey"].uInt = sendKey
      
      let json = sendData.rawString([.castNilToNSNull: true])!
      outputStream!.write(json, maxLength: json.utf8.count)
      
      sendKey += 1
      if sendKey >= UInt.max {
        sendKey = 0
      }
      
    } else {
      print("Socket not connected.")
    }
  }
}
