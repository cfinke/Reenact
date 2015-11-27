//
//  CaptureController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class CaptureController: UIViewController {
    // MARK: Properties
    
    @IBOutlet weak var originalPhotoOverlay: UIImageView!
    
    var originalPhoto:UIImage?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        print("Capture Controller did load")
        // Set originalPhotoOverlay to contain the chosen image.
        originalPhotoOverlay.image = self.originalPhoto
        print("Set image")
        print(self.originalPhoto)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Actions
    
    // MARK: Delegates
    
}

