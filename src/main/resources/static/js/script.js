const toggleSidebar = () => {
    if($(".sidebar").is(":visible"))
    {
        $(".sidebar").css("display","none");
        $(".content").css("margin-left","1%");

    }
    else
    {
        $(".sidebar").css("display","block");
        $(".content").css("margin-left","20%");
    }
};

const search = () =>
{
    let query=$("#search-input").val()

    if(query=='')
    {

        $(".search-result").hide();
    }
    else{
        console.log(query);
        //sending request to server
        let url=`http://localhost:8080/search/${query}`;
        fetch(url).then((response)=> {
            return response.json();
        }).then((data)=>{
            let text=`<div class='list-group'>`;
            data.forEach((contact) => {
                text+=`<a href='/user/${contact.cId}/contact' class='list-group-item list-group-item-action'>${contact.name}</a>`
            });

            text+='</div>'
            $(".search-result").html(text);
            $(".search-result").show(); 

            //console.log(data);
        });
        
    }
}

//first request to server to create order

const paymentStart=()=>
{
    console.log("payment started..");
    let amount=$("#patment_field").val();
    console.log(amount);
    if(amount=='' || amount==null)
    {
        alert("amount is required !!");
        return;
    }
    // we are using ajax to send request  to server to create order :-jquery

    $.ajax(
        {
            url: '/user/create_order',
            data:JSON.stringify({amount:amount,info:'order_request'}) ,
            contentType:'application/json',
            type:'POST',
            dataType:'json',
            success:function(response){
                //invoke when success
                console.log(response)
                if(response.status=="created")
                {
                    let options={
                        key:'rzp_test_v4fLZnccQkwZzP',
                        amount:response.amount,
                        currency:'INR',
                        name:"SMart Contect Manager",
                        description: "Donation",
                        image:"https://cdn.pixabay.com/photo/2021/03/19/13/40/online-6107598__340.png",
                        order_id:response.id,
                        handler:function(response){
                            console.log(response.razorpay_payment_id)
                            console.log(response.razorpay_order_id)
                            console.log(response.razorpay_signature)
                            console.log("payment successfull")
                            alert("congrates!!!")
                        },

                        prefill: {
                            name: "",
                            email: "",
                            contact: ""
                            },
                        notes: {
                        address: "krutarth khatri world"
                        
                        },
                        theme: {
                        color: "#3399cc"
                        }


                    };
                    let rzp=new Razorpay(options);
                   
                    rzp.on('payment.failed', function (response){
                        console.log(response.error.code);
                        console.log(response.error.description);
                        console.log(response.error.source);
                        console.log(response.error.step);
                        console.log(response.error.reason);
                        console.log(response.error.metadata.order_id);
                        console.log(response.error.metadata.payment_id);
                        alert("oops payment failed..");
                        }); 
                        rzp.open();
                }
            },
            error:function(error)
            {
                //invoke when error
                console.log(error);
                alert("somthing went wrong..");
            }


        }
    )


};