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

        buildLayout(view.bounds.size)
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
    
    override func buildLayout(size: CGSize) {
        super.buildLayout(size)
        
        // Load the comparison image.
        comparisonImage.image = combinedPhoto
        comparisonImage.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            comparisonImage.frame = CGRect(
                x: 0,
                y: 0,
                width: Int(size.width),
                height: Int(size.height - CGFloat(buttonContainerSize))
            )
        }
        else {
            // Landscape orientation.
            comparisonImage.frame = CGRect(
                x: 0,
                y: 0,
                width: Int(size.width - CGFloat(buttonContainerSize)),
                height: Int(size.height)
            )
        }
        
        view.addSubview(comparisonImage)
        
        // Add share button
        let shareButtonImage = UIImage(named: "share.png")
        shareButton.setImage(shareButtonImage, forState: .Normal)
        shareButton.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            shareButton.frame = CGRect(
                x: Int(round(size.width / 2) - round(smallButtonSize / 2)),
                y: Int(size.height - buttonContainerSize + round(smallButtonSize / 2)),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        else {
            // Landscape
            shareButton.frame = CGRect(
                x: Int(size.width - buttonContainerSize + round(smallButtonSize / 2)),
                y: Int(round(size.height / 2) - round(smallButtonSize / 2)),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        
        shareButton.addTarget(self, action:"share:", forControlEvents: .TouchUpInside)
        view.addSubview(shareButton)
        
        // Add restart button.
        let restartButtonImage = UIImage(named: "replay.png")
        restartButton.setImage(restartButtonImage, forState: .Normal)
        restartButton.contentMode = .ScaleAspectFit
        restartButton.alpha = 0.75;
        restartButton.backgroundColor = UIColor.blackColor()
        
        restartButton.frame = CGRect(
            x: 30,
            y: 30,
            width: smallButtonSize,
            height: smallButtonSize
        )
        
        restartButton.addTarget(self, action:"startOver:", forControlEvents: .TouchUpInside)
        view.addSubview(restartButton)
        
    }
}

