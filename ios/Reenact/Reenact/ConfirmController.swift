//
//  ConfirmController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class ConfirmController: ReenactControllerBase {
    // MARK: Properties
    
    var originalPhoto: UIImage?
    var newPhoto: UIImage?

    let compareOriginal: UIImageView = UIImageView()
    let compareNew: UIImageView = UIImageView()
    let confirmButton: UIButton = UIButton()
    let cancelButton: UIButton = UIButton()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        compareOriginal.image = originalPhoto
        compareOriginal.contentMode = .ScaleAspectFit
        
        if (view.bounds.size.width < view.bounds.size.height) {
            let compareOriginalWidth = round( view.bounds.size.width / 2 )
            
            compareOriginal.frame = CGRect(
                x: 0,
                y: 0,
                width: compareOriginalWidth,
                height: view.bounds.height - 100
            )
        
            // Portrait orientation.
        }
        else {
            // Landscape orientation.
            
        }
        
        view.addSubview(compareOriginal)
        
        compareNew.image = newPhoto
        compareNew.contentMode = .ScaleAspectFit
        
        if (view.bounds.size.width < view.bounds.size.height) {
            // Portrait orientation.
            let compareNewWidth = round( view.bounds.size.width / 2 )
            
            compareNew.frame = CGRect(
                x: round(view.bounds.width / 2),
                y: 0,
                width: compareNewWidth,
                height: view.bounds.height - 100
            )
        }
        else {
            // Landscape orientation.
        }
        
        view.addSubview(compareNew)
        
        let buttonContainerSize = 100
        
        // Add confirm button.
        let confirmButtonImage = UIImage(named: "checkmark.png")
        let confirmButtonSize = buttonContainerSize
        confirmButton.setImage(confirmButtonImage, forState: .Normal)
        confirmButton.contentMode = .ScaleAspectFit
        
        if (view.bounds.size.width < view.bounds.size.height) {
            // Portrait orientation.
            confirmButton.frame = CGRect(
                x: Int(round(view.bounds.width / 2) - round(CGFloat(confirmButtonSize) / 2)),
                y: Int(view.bounds.height - CGFloat(buttonContainerSize)),
                width: confirmButtonSize,
                height: confirmButtonSize
            )
        }
        else {
            // Landscape
            confirmButton.frame = CGRect(
                x: Int(view.bounds.width - CGFloat(buttonContainerSize)),
                y: Int(round(view.bounds.height / 2) - round(CGFloat(confirmButtonSize) / 2)),
                width: confirmButtonSize,
                height: confirmButtonSize
            )
        }
        
        confirmButton.addTarget(self, action:"confirmShot:", forControlEvents: .TouchUpInside)
        view.addSubview(confirmButton)
        
        // Add switch button.
        let cancelButtonImage = UIImage(named: "back.png")
        cancelButton.setImage(cancelButtonImage, forState: .Normal)
        cancelButton.contentMode = .ScaleAspectFit
        
        if (view.bounds.size.width < view.bounds.size.height) {
            // Portrait orientation.
            cancelButton.frame = CGRect(
                x: Int(round(view.bounds.width / 6 * 1) - round(CGFloat(smallButtonSize) / 2)),
                y: Int(
                    view.bounds.height -
                        CGFloat(buttonContainerSize) +
                        round(CGFloat(buttonContainerSize - smallButtonSize) / 2)
                ),
                width: smallButtonSize,
                height: smallButtonSize
            )
        }
        else {
            // Landscape
            cancelButton.frame = CGRect(
                x: Int(
                    view.bounds.width -
                        CGFloat(buttonContainerSize) +
                        round(CGFloat(buttonContainerSize - smallButtonSize) / 2)
                ),
                y: Int(round(view.bounds.height / 6 * 1) - round(CGFloat(smallButtonSize) / 2)),
                width: smallButtonSize,
                height: smallButtonSize
            )
        }
        
        cancelButton.addTarget(self, action:"cancelConfirmation:", forControlEvents: .TouchUpInside)
        view.addSubview(cancelButton)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject!) {
        print("In prepareForSegue")
        
        if (segue.identifier == "confirmToShare") {
            // let svc = segue.destinationViewController as! ShareController;
        }
    }
    
    
    // MARK: Actions
    
    func confirmShot(sender: UIButton) {
        // Merge the two images.
        
        // Send the final image off to the share controller.
        // self.performSegueWithIdentifier("confirmToShare", sender: self)
    }
    
    func cancelConfirmation(sender: UIButton) {
        // Go back to the capture view.
        self.performSegueWithIdentifier("backToCapture", sender: self)
    }
    
    // MARK: Delegates
    
}

