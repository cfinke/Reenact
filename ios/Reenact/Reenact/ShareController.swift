//
//  ShareController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class ShareController: ReenactControllerBase {
    // MARK: Properties
    
    var combinedPhoto: UIImage?
    
    let comparisonImage: UIImageView = UIImageView()
    let shareButton: UIButton = UIButton()
    let restartButton: UIButton = UIButton()

    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Load the comparison image.
        comparisonImage.image = combinedPhoto
        comparisonImage.contentMode = .ScaleAspectFit
        
        if (view.bounds.size.width < view.bounds.size.height) {
            // Portrait orientation.
            comparisonImage.frame = CGRect(
                x: 0,
                y: 0,
                width: Int(view.bounds.size.width),
                height: Int(view.bounds.height - CGFloat(buttonContainerSize))
            )
        }
        else {
            // Landscape orientation.
            comparisonImage.frame = CGRect(
                x: 0,
                y: 0,
                width: Int(view.bounds.width - CGFloat(buttonContainerSize)),
                height: Int(view.bounds.height)
            )
        }
        
        view.addSubview(comparisonImage)
        
        // Add share button
        let shareButtonImage = UIImage(named: "share.png")
        shareButton.setImage(shareButtonImage, forState: .Normal)
        shareButton.contentMode = .ScaleAspectFit
        
        if (view.bounds.size.width < view.bounds.size.height) {
            // Portrait orientation.
            shareButton.frame = CGRect(
                x: Int(round(view.bounds.width / 2) - round(CGFloat(buttonContainerSize) / 2)),
                y: Int(view.bounds.height - CGFloat(buttonContainerSize)),
                width: buttonContainerSize,
                height: buttonContainerSize
            )
        }
        else {
            // Landscape
            shareButton.frame = CGRect(
                x: Int(view.bounds.width - CGFloat(buttonContainerSize)),
                y: Int(round(view.bounds.height / 2) - round(CGFloat(buttonContainerSize) / 2)),
                width: buttonContainerSize,
                height: buttonContainerSize
            )
        }
        
        shareButton.addTarget(self, action:"share:", forControlEvents: .TouchUpInside)
        view.addSubview(shareButton)
        
        print(combinedPhoto)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Actions
    func startOver(sender: UIButton!){
        self.performSegueWithIdentifier("shareToIntro", sender: self)
    }
    
    func share(sender: UIButton!) {
        let objectsToShare = [combinedPhoto!]
        let activityVC = UIActivityViewController(activityItems: objectsToShare, applicationActivities: nil)
            
        self.presentViewController(activityVC, animated: true, completion: nil)
    }
    
    // MARK: Delegates
    
}

