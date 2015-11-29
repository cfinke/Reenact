//
//  IntroController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class IntroController: ReenactControllerBase, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    // MARK: Properties
    
    let picker = UIImagePickerController()
    
    var originalPhoto:UIImage?
    
    let choosePhotoButton: UIButton = UIButton()
    let reenactLogoView: UIImageView = UIImageView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        picker.delegate = self

        buildLayout(view.bounds.size)
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject!) {
        if (segue.identifier == "introToCapture") {
            print("In prepareForSegue")
            let svc = segue.destinationViewController as! CaptureController;
            
            svc.originalPhoto = self.originalPhoto
        }
    }

    func chooseOriginalPhoto(sender: UIButton!) {
        picker.allowsEditing = false
        picker.sourceType = .PhotoLibrary
        presentViewController(picker, animated: true, completion: nil)
    }
    
    //MARK: Actions
    @IBAction func unwindToIntro(segue: UIStoryboardSegue) {
    }
    
    //MARK: Delegates
    func imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : AnyObject]) {
        let chosenImage = info[UIImagePickerControllerOriginalImage] as! UIImage
        dismissViewControllerAnimated(true, completion: nil)
        
        // Show the next view and set the camera background image to chosenImage
        self.originalPhoto = chosenImage
        self.performSegueWithIdentifier("introToCapture", sender: self)
    }
    
    func imagePickerControllerDidCancel(picker: UIImagePickerController) {
        dismissViewControllerAnimated(true, completion: nil)
      
    }
    
    func showHelp(sender: UIButton!) {
        self.performSegueWithIdentifier("introToHelp", sender: self)
    }
    
    override func buildLayout(size: CGSize) {
        super.buildLayout(size)
        
        // Add the logo on the intro screen.
        let reenactLogo = UIImage(named:"logo.png")
        reenactLogoView.image = reenactLogo
        reenactLogoView.contentMode = .ScaleAspectFit
        
        let reenactLogoWidth = round( min( size.width, size.height ) / 3 )
        reenactLogoView.frame = CGRect(
            x: round((size.width / 2) - (reenactLogoWidth / 2)),
            y: round(size.height / 4),
            width: reenactLogoWidth,
            height: reenactLogoWidth
        )
        
        view.addSubview(reenactLogoView)
        
        choosePhotoButton.setTitle("CHOOSE A PHOTO TO REENACT", forState: .Normal)
        choosePhotoButton.setTitleColor(UIColor.whiteColor(), forState: .Normal)
        choosePhotoButton.backgroundColor = UIColor.blackColor()
        choosePhotoButton.contentEdgeInsets = UIEdgeInsetsMake(7.0, 7.0, 7.0, 7.0)
        choosePhotoButton.addTarget(self, action:"chooseOriginalPhoto:", forControlEvents: .TouchUpInside)
        choosePhotoButton.titleLabel!.font = UIFont(name:"Helvetica Neue", size: 12)
        choosePhotoButton.frame = CGRect(
            x: round((size.width - 240) / 2),
            y: round(size.height * 0.75),
            width: 240,
            height: 30
        )
        // @todo Use auto-layout to center this button horizontally and size it vertically and horizontally.
        
        view.addSubview(choosePhotoButton)
    }
}

