//
//  CaptureController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit
import AVFoundation

class CaptureController: ReenactControllerBase {
    // MARK: Properties
    
    var originalPhoto: UIImage?
    var newPhoto: UIImage?
    
    let captureSession = AVCaptureSession()
    
    var captureDevice: AVCaptureDevice?
    var captureDevices: [AVCaptureDevice] = []
    var cameraIndex: Int = 0
    var deviceInput: AVCaptureDeviceInput?
    let stillImageOutput = AVCaptureStillImageOutput()
    
    // UI Elements
    let originalPhotoOverlay: UIImageView = UIImageView()
    let captureButton: UIButton = UIButton()
    let switchCameraButton: UIButton = UIButton()
    let cancelButton: UIButton = UIButton()
    
    let previewLayer: AVCaptureVideoPreviewLayer = AVCaptureVideoPreviewLayer()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        if originalPhoto!.size.width <= originalPhoto!.size.height {
            print("Setting to portrait")
            UIDevice.currentDevice().setValue(UIInterfaceOrientation.Portrait.rawValue, forKey: "orientation")
        }
        else {
            print("Setting to landscape")
            UIDevice.currentDevice().setValue(UIInterfaceOrientation.LandscapeLeft.rawValue, forKey: "orientation")
        }
        
        let devices = AVCaptureDevice.devices()
        
        for device in devices {
            // Make sure this particular device supports video (which apparently implies support for photos)
            
            if (device.hasMediaType(AVMediaTypeVideo)) {
                captureDevices.append(device as! AVCaptureDevice)
            }
        }

        buildLayout(view.bounds.size)
        
        // Start fading the overlay image.
        startImageFade()
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject!) {
        print("In prepareForSegue")
        
        if (segue.identifier == "captureToConfirm") {
            let svc = segue.destinationViewController as! ConfirmController;
            
            svc.originalPhoto = self.originalPhoto
            svc.newPhoto = self.newPhoto
        }
    }
    
    override func shouldAutorotate() -> Bool {
        return false
    }
    
    override func supportedInterfaceOrientations() -> UIInterfaceOrientationMask {
        if originalPhoto!.size.width <= originalPhoto!.size.height {
            return [UIInterfaceOrientationMask.Portrait]
        }
        else {
            return [UIInterfaceOrientationMask.Landscape]
        }
    }
    
    func startImageFade() {
        fadeOut()
    }
    
    func fadeOut() {
        UIView.animateWithDuration( 5.0, delay: 0, options: UIViewAnimationOptions.CurveEaseOut, animations : {
            self.originalPhotoOverlay.alpha = 0;
            }, completion : {
                (finished: Bool) -> Void in
                self.fadeIn()
            }
        )
    }
    
    func fadeIn() {
        UIView.animateWithDuration( 5.0, delay: 0, options: UIViewAnimationOptions.CurveEaseIn, animations : {
            self.originalPhotoOverlay.alpha = 0.75
            }, completion : {
                (finished: Bool) -> Void in
                self.fadeOut()
            }
        )
        
    }
    
    func getSelectedCamera() -> AVCaptureDevice? {
        if cameraIndex < captureDevices.count {
            return captureDevices[cameraIndex]
        }
        
        return nil
    }
    
    func beginSession() {
        captureDevice = getSelectedCamera()
        
        if (nil == captureDevice) {
            print("The camera wouldn't start.")
            return
        }
        
        captureSession.sessionPreset = AVCaptureSessionPresetLow

        do {
            try deviceInput = AVCaptureDeviceInput(device: captureDevice)
        } catch _ {
            print("Reenact couldn't load the camera preview.")
            return
        }
        
        captureSession.addInput(deviceInput)

        
        let bounds = self.originalPhotoOverlay.layer.bounds;
        
        captureSession.sessionPreset = AVCaptureSessionPresetPhoto
        captureSession.startRunning()
        stillImageOutput.outputSettings = [AVVideoCodecKey:AVVideoCodecJPEG]
        if captureSession.canAddOutput(stillImageOutput) {
            captureSession.addOutput(stillImageOutput)
        }
        
        previewLayer.session = captureSession
        
        previewLayer.bounds = CGRectMake(0.0, 0.0, bounds.size.width, bounds.size.height)
        
        previewLayer.position = CGPointMake(bounds.midX, bounds.midY)
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        let cameraPreview = UIView(frame: CGRectMake(0.0, 0.0, bounds.size.width, bounds.size.height))
        
        let deviceOrientation = UIDevice.currentDevice().orientation
        
        if deviceOrientation == UIDeviceOrientation.LandscapeLeft {
            previewLayer.connection.videoOrientation = AVCaptureVideoOrientation.LandscapeLeft
        }
        else if deviceOrientation == UIDeviceOrientation.LandscapeRight {
            previewLayer.connection.videoOrientation = AVCaptureVideoOrientation.LandscapeRight
        }
        else if deviceOrientation == UIDeviceOrientation.PortraitUpsideDown {
            previewLayer.connection.videoOrientation = AVCaptureVideoOrientation.PortraitUpsideDown
        }
        
        
        cameraPreview.layer.addSublayer(previewLayer)
        view.addSubview(cameraPreview)
        view.sendSubviewToBack(cameraPreview)
    }
    
    func endSession() {
        captureSession.removeInput(deviceInput)
        
        captureSession.stopRunning()
    }
    
    // MARK: Actions
    @IBAction func unwindToCapture(segue: UIStoryboardSegue) {
        
    }
    
    func takePicture(sender: UIButton!) {
        print("Taking a picture.")
        
        // Make the camera set the orientation of the image when it's taken.
        let videoOrientation = previewLayer.connection.videoOrientation
        stillImageOutput.connectionWithMediaType(AVMediaTypeVideo).videoOrientation = videoOrientation
        
        if let videoConnection = stillImageOutput.connectionWithMediaType(AVMediaTypeVideo) {
            stillImageOutput.captureStillImageAsynchronouslyFromConnection(videoConnection) {
                (imageDataSampleBuffer, error) -> Void in
                let imageData = AVCaptureStillImageOutput.jpegStillImageNSDataRepresentation(imageDataSampleBuffer)
                self.newPhoto = UIImage(data: imageData)
                self.performSegueWithIdentifier("captureToConfirm", sender: self)
            }
        }
    }
    
    func switchCamera(sender: UIButton!) {
        print("Switching camera")
        endSession()
        print("Ended session")
        cameraIndex++
        cameraIndex %= captureDevices.count
        print("Camera Index is now")
        print(cameraIndex)
        beginSession()
        
    }
    
    func cancelCapture(sender: UIButton!) {
        self.performSegueWithIdentifier("captureToIntro", sender: self)
    }
    
    // MARK: Delegates
    
    override func buildLayout(size: CGSize) {
        super.buildLayout(size)
        
        // Add the original image overlay
        originalPhotoOverlay.image = originalPhoto
        originalPhotoOverlay.contentMode = .ScaleAspectFit
        originalPhotoOverlay.alpha = 0.75
        
        if (size.width < size.height) {
            // Portrait orientation.
            originalPhotoOverlay.frame = CGRect(
                x: 0,
                y: 0,
                width: size.width,
                height: size.height - CGFloat(buttonContainerSize)
            )
        }
        else {
            // Landscape orientation.
            originalPhotoOverlay.frame = CGRect(
                x: 0,
                y: 0,
                width: Int(Float(size.width) - Float(buttonContainerSize)),
                height: Int(size.height)
            )
        }
        
        view.addSubview(originalPhotoOverlay)
        
        // Add capture button.
        let captureButtonImage = UIImage(named: "camera.png")
        captureButton.setImage(captureButtonImage, forState: .Normal)
        captureButton.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            
            captureButton.frame = CGRect(
                x: Int(round(size.width / 2) - round(buttonContainerSize / 2)),
                y: Int(size.height - buttonContainerSize),
                width: Int(buttonContainerSize),
                height: Int(buttonContainerSize)
            )
        }
        else {
            // Landscape
            captureButton.frame = CGRect(
                x: Int(size.width - buttonContainerSize),
                y: Int(round(size.height / 2) - round(buttonContainerSize / 2)),
                width: Int(buttonContainerSize),
                height: Int(buttonContainerSize)
            )
        }
        
        captureButton.addTarget(self, action:"takePicture:", forControlEvents: .TouchUpInside)
        view.addSubview(captureButton)
        
        if captureDevices.count > 1 {
            // Add switch button.
            let switchCameraButtonImage = UIImage(named: "camera-switch.png")
            switchCameraButton.setImage(switchCameraButtonImage, forState: .Normal)
            switchCameraButton.contentMode = .ScaleAspectFit
            
            if (size.width < size.height) {
                // Portrait orientation.
                switchCameraButton.frame = CGRect(
                    x: Int(round(size.width / 6 * 5) - round(smallButtonSize / 2)),
                    y: Int(
                        size.height -
                            buttonContainerSize +
                            round(CGFloat(buttonContainerSize - smallButtonSize) / 2)
                    ),
                    width: Int(smallButtonSize),
                    height: Int(smallButtonSize)
                )
            }
            else {
                // Landscape
                switchCameraButton.frame = CGRect(
                    x: Int(
                        size.width -
                            buttonContainerSize +
                            round((buttonContainerSize - smallButtonSize) / 2)
                    ),
                    y: Int(round(size.height / 6 * 5) - round(smallButtonSize / 2)),
                    width: Int(smallButtonSize),
                    height: Int(smallButtonSize)
                )
            }
            
            switchCameraButton.addTarget(self, action:"switchCamera:", forControlEvents: .TouchUpInside)
            view.addSubview(switchCameraButton)
        }
        
        let cancelButtonImage = UIImage(named: "back.png")
        cancelButton.setImage(cancelButtonImage, forState: .Normal)
        cancelButton.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            cancelButton.frame = CGRect(
                x: Int(round(size.width / 6 * 1) - round(smallButtonSize / 2)),
                y: Int(
                    size.height -
                        CGFloat(buttonContainerSize) +
                        round((buttonContainerSize - smallButtonSize) / 2)
                ),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        else {
            // Landscape
            cancelButton.frame = CGRect(
                x: Int(
                    size.width -
                        buttonContainerSize +
                        round((buttonContainerSize - smallButtonSize) / 2)
                ),
                y: Int(round(size.height / 6 * 1) - round(smallButtonSize / 2)),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        
        cancelButton.addTarget(self, action:"cancelCapture:", forControlEvents: .TouchUpInside)
        view.addSubview(cancelButton)
        
        endSession()
        beginSession()
    }
}

