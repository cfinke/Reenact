//
//  ShareController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class HelpController: ReenactControllerBase {
    // MARK: Properties
    
    let closeButton: UIButton = UIButton()
    let reenactLogoView: UIImageView = UIImageView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        buildLayout(view.bounds.size)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Actions
    func closeHelp(sender: UIButton!) {
        self.performSegueWithIdentifier("helpToIntro", sender: self)
    }
    
    override func buildLayout(size: CGSize) {
        super.buildLayout(size)
        
        let closeButtonImage = UIImage(named:"close.png")
        let closeButtonSize = 30
        let closeButtonOffset = 30
        closeButton.setImage(closeButtonImage, forState: .Normal)
        closeButton.frame = CGRect(
            x: Int(size.width - CGFloat(closeButtonOffset) - CGFloat(closeButtonSize)),
            y: closeButtonOffset,
            width: closeButtonSize,
            height: closeButtonSize
        )
        closeButton.contentMode = .ScaleAspectFit
        closeButton.addTarget(self, action:"closeHelp:", forControlEvents: .TouchUpInside)
        view.addSubview(closeButton)
        
        let instructionString = "Reenact\n\nReenact photos with Reenact. Choose a photo, align the camera, and take the shot.\n\nFor help, email help@reenact.me\n\n@ReenactApp on Twitter"
        
        let reenactLogo = UIImage(named:"logo.png")
        reenactLogoView.image = reenactLogo
        reenactLogoView.contentMode = .ScaleAspectFit
        
        let reenactLogoWidth = round( min( size.width, size.height ) / 3 )
        let textMargin = 30
        
        let instructions = UITextView()
        instructions.text = instructionString
        instructions.textColor = UIColor.whiteColor()
        instructions.backgroundColor = UIColor.blackColor()
        instructions.font = UIFont(name:"Helvetica Neue", size: 16.0)

        
        if (size.width <= size.height) {
            // Add the logo on the intro screen.
            reenactLogoView.frame = CGRect(
                x: round((size.width / 2) - (reenactLogoWidth / 2)),
                y: round(size.height / 4),
                width: reenactLogoWidth,
                height: reenactLogoWidth
            )
            
            instructions.frame = CGRect(
                x: textMargin,
                y: Int(round(size.height / 2)) + 25,
                width: Int(size.width) - (textMargin * 2),
                height: 200
            )
        }
        else {
            reenactLogoView.frame = CGRect(
                x: 0,
                y: 0,
                width: size.width / 2,
                height: size.height
            )
            
            instructions.frame = CGRect(
                x: textMargin + Int(round(size.width / 2)),
                y: textMargin,
                width: Int(round(size.width / 2)) - (textMargin * 2),
                height: Int(size.height) - (textMargin * 2)
            )
        }
        
        view.addSubview(reenactLogoView)
        view.addSubview(instructions)

    }
    
}

